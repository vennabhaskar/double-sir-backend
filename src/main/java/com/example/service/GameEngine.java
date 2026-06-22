package com.example.doublesir.service;

import com.example.doublesir.model.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Server-side authority for one room's game state.
 * Implements the Double Sir / Court Piece style flow, with a simplified version of your rules.
 */
public class GameEngine {

    private final GameState state;
    private final SecureRandom random = new SecureRandom();

    public GameEngine(String roomId) {
        this.state = new GameState();
        this.state.setRoomId(roomId);
        initDeck();
    }

    public GameState getState() {
        return state;
    }

    private void initDeck() {
        state.getDeck().clear();
        for (CardSuit suit : CardSuit.values()) {
            for (CardRank rank : CardRank.values()) {
                String id = suit.name() + "-" + rank.name() + "-" + random.nextInt(1_000_000);
                state.getDeck().add(new Card(id, suit, rank, true));
            }
        }
        Collections.shuffle(state.getDeck(), random);
    }

    public synchronized Player addPlayer(String sessionId, String name) {
        if (state.getPlayers().size() >= 4) {
            throw new IllegalStateException("Room is full");
        }
        Player player = new Player();
        player.setId(UUID.randomUUID().toString());
        player.setSessionId(sessionId);
        player.setName(name);

        PlayerPosition position = switch (state.getPlayers().size()) {
            case 0 -> PlayerPosition.NORTH;
            case 1 -> PlayerPosition.EAST;
            case 2 -> PlayerPosition.SOUTH;
            case 3 -> PlayerPosition.WEST;
            default -> throw new IllegalStateException("Unexpected player index");
        };
        player.setPosition(position);
        player.setTeam((position == PlayerPosition.NORTH || position == PlayerPosition.SOUTH) ? "A" : "B");
        player.setTrumpKeeper(position == PlayerPosition.NORTH);

        state.getPlayers().add(player);

        if (state.getPlayers().size() == 4) {
            startTrumpSelection();
        }
        return player;
    }

    private void startTrumpSelection() {
        state.setPhase(GamePhase.TRUMP_SELECTION);
        // first 5 cards each: trump keeper chooses trump from their hand in UI
        for (Player p : state.getPlayers()) {
            for (int i = 0; i < 5; i++) {
                p.getHand().add(drawCard());
            }
        }
    }

    private Card drawCard() {
        if (state.getDeck().isEmpty()) return null;
        return state.getDeck().remove(0);
    }

    public synchronized void selectTrump(CardSuit suit, String trumpKeeperId) {
        if (state.getPhase() != GamePhase.TRUMP_SELECTION) {
            throw new IllegalStateException("Not in trump selection phase");
        }
        Player keeper = state.getPlayers().stream()
                .filter(Player::isTrumpKeeper)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No trump keeper"));
        if (!keeper.getId().equals(trumpKeeperId)) {
            throw new IllegalStateException("Only trump keeper can choose trump");
        }
        state.setTrumpSuit(suit);
        // pick one card of that suit as trump card and keep aside (first match)
        Card trumpCard = keeper.getHand().stream()
                .filter(c -> c.getSuit() == suit)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Keeper has no card of selected suit"));
        state.setTrumpCard(trumpCard);
        keeper.getHand().remove(trumpCard);

        // deal remaining cards: keeper gets 8 more, others get 8 (for 13 total)
        for (Player p : state.getPlayers()) {
            int toDeal = p.isTrumpKeeper() ? 8 : 8;
            for (int i = 0; i < toDeal; i++) {
                Card c = drawCard();
                if (c != null) {
                    p.getHand().add(c);
                }
            }
        }
        state.setPhase(GamePhase.PLAYING);
        state.setCurrentTurn(PlayerPosition.NORTH);
        state.setTrickLeader(PlayerPosition.NORTH);
    }

    public synchronized void reconnect(String sessionId, String playerId) {
        state.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .ifPresent(p -> {
                    p.setConnected(true);
                    p.setSessionId(sessionId);
                });
    }

    public synchronized void markDisconnected(String sessionId) {
        state.getPlayers().stream()
                .filter(p -> sessionId.equals(p.getSessionId()))
                .findFirst()
                .ifPresent(p -> p.setConnected(false));
    }

    public synchronized void playCard(String playerId, String cardId) {
        if (state.getPhase() != GamePhase.PLAYING) {
            throw new IllegalStateException("Not in playing phase");
        }
        Player player = state.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player"));

        if (player.getPosition() != state.getCurrentTurn()) {
            throw new IllegalStateException("Not this player's turn");
        }

        Card card = player.getHand().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not in hand"));

        // Basic follow-suit enforcement (simplified). Detailed ace/trump rules can be extended.
        Trick trick = state.getCurrentTrick();
        if (trick.getCards().isEmpty()) {
            trick.setLeadingSuit(card.getSuit());
        } else {
            CardSuit leading = trick.getLeadingSuit();
            boolean hasLeading = player.getHand().stream().anyMatch(c -> c.getSuit() == leading);
            if (hasLeading && card.getSuit() != leading) {
                throw new IllegalStateException("Must follow leading suit if possible");
            }
        }

        // remove from hand and add to trick
        player.getHand().remove(card);
        boolean trumpPlay = state.isTrumpRevealed() && card.getSuit() == state.getTrumpSuit();
        PlayedCard played = new PlayedCard(player.getId(), player.getPosition(), card, trumpPlay, System.currentTimeMillis());
        trick.getCards().add(played);

        // Trump reveal rule (simplified): opponent off-suit reveals trump
        if (!state.isTrumpRevealed() && trick.getLeadingSuit() != null) {
            CardSuit leading = trick.getLeadingSuit();
            boolean hasLeading = player.getHand().stream().anyMatch(c -> c.getSuit() == leading);
            boolean isOpponentTeam = !Objects.equals(player.getTeam(), teamOf(state.getTrickLeader()));
            if (!hasLeading && isOpponentTeam) {
                state.setTrumpRevealed(true);
            }
        }

        // advance turn or resolve trick
        state.setCurrentTurn(state.getCurrentTurn().next());
        if (trick.getCards().size() == 4) {
            resolveTrick();
        }
    }

    private String teamOf(PlayerPosition pos) {
        return (pos == PlayerPosition.NORTH || pos == PlayerPosition.SOUTH) ? "A" : "B";
    }

    private void resolveTrick() {
        Trick trick = state.getCurrentTrick();
        PlayerPosition winnerPos = evaluateTrickWinner(trick);
        trick.setWinner(winnerPos);
        trick.setComplete(true);
        state.getCompletedTricks().add(trick);

        // add cards to table pile
        state.getTablePile().addAll(trick.getCards());

        // Double Sir style consecutive-win logic, with ace exception.
        if (state.getLastTrickWinner() == winnerPos) {
            state.setConsecutiveWins(state.getConsecutiveWins() + 1);
            Card winningCard = trick.getCards().stream()
                    .filter(pc -> pc.getPosition() == winnerPos)
                    .map(PlayedCard::getCard)
                    .max(Comparator.comparingInt(c -> c.getRank().getPower()))
                    .orElse(null);
            boolean isAce = winningCard != null && winningCard.getRank() == CardRank.ACE;

            if (state.getConsecutiveWins() >= 2) {
                if (isAce) {
                    // second win with ace does not capture pile, but keeps streak notion
                    state.setLastWinWasAce(true);
                    state.setConsecutiveWins(1);
                } else {
                    capturePile(winnerPos);
                    state.setConsecutiveWins(0);
                    state.setLastWinWasAce(false);
                }
            }
        } else {
            state.setConsecutiveWins(1);
            state.setLastWinWasAce(false);
        }

        state.setLastTrickWinner(winnerPos);
        state.setTrickLeader(winnerPos);
        state.setCurrentTurn(winnerPos);

        // reset current trick
        state.setPhase(GamePhase.TRICK_COMPLETE);
        state.setPhase(GamePhase.PLAYING);
        Trick next = new Trick();
        state.setTrumpRevealed(state.isTrumpRevealed());
        // replace current trick
        GameState newState = state; // alias
        newState.getCurrentTrick().getCards().clear();
        newState.getCurrentTrick().setLeadingSuit(null);
        newState.getCurrentTrick().setWinner(null);
        newState.getCurrentTrick().setComplete(false);

        // check end-of-game condition
        boolean allEmpty = state.getPlayers().stream().allMatch(p -> p.getHand().isEmpty());
        if (allEmpty) {
            endGame();
        }
    }

    private PlayerPosition evaluateTrickWinner(Trick trick) {
        CardSuit leading = trick.getLeadingSuit();
        CardSuit trump = state.getTrumpSuit();
        boolean trumpRevealed = state.isTrumpRevealed();

        PlayedCard best = trick.getCards().get(0);
        for (PlayedCard pc : trick.getCards()) {
            if (beats(pc.getCard(), best.getCard(), leading, trump, trumpRevealed)) {
                best = pc;
            }
        }
        return best.getPosition();
    }

    private boolean beats(Card c1, Card c2, CardSuit leading, CardSuit trump, boolean trumpRevealed) {
        boolean c1Trump = trumpRevealed && c1.getSuit() == trump;
        boolean c2Trump = trumpRevealed && c2.getSuit() == trump;
        boolean c1Leading = c1.getSuit() == leading;
        boolean c2Leading = c2.getSuit() == leading;

        if (c1Trump && !c2Trump) return true;
        if (!c1Trump && c2Trump) return false;
        if (c1Trump && c2Trump) return c1.getRank().getPower() > c2.getRank().getPower();

        if (c1Leading && !c2Leading) return true;
        if (!c1Leading && c2Leading) return false;

        if (c1.getSuit() == c2.getSuit()) {
            return c1.getRank().getPower() > c2.getRank().getPower();
        }
        return false;
    }

    private void capturePile(PlayerPosition winnerPos) {
        String team = teamOf(winnerPos);
        int hands = state.getTablePile().size() / 4; // number of tricks represented
        if ("A".equals(team)) {
            state.setTeamAHnds(state.getTeamAHnds() + hands);
        } else {
            state.setTeamBHands(state.getTeamBHands() + hands);
        }
        state.getTablePile().clear();
    }

    private void endGame() {
        state.setPhase(GamePhase.GAME_OVER);
        if (state.getTeamAHnds() > state.getTeamBHands()) {
            state.setWinningTeam("A");
        } else if (state.getTeamBHands() > state.getTeamAHnds()) {
            state.setWinningTeam("B");
        } else {
            state.setWinningTeam(null); // draw
        }
    }

    /**
     * Build a client-safe view of state where other players' cards are hidden.
     */
    public Map<String, Object> buildClientView(String playerId) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("roomId", state.getRoomId());
        view.put("phase", state.getPhase().name());
        view.put("trumpSuit", state.getTrumpSuit() != null ? state.getTrumpSuit().name() : null);
        view.put("trumpRevealed", state.isTrumpRevealed());
        view.put("currentTurn", state.getCurrentTurn().name());
        view.put("trickLeader", state.getTrickLeader().name());
        view.put("teamAHnds", state.getTeamAHnds());
        view.put("teamBHands", state.getTeamBHands());
        view.put("consecutiveWins", state.getConsecutiveWins());
        view.put("lastTrickWinner", state.getLastTrickWinner() != null ? state.getLastTrickWinner().name() : null);
        view.put("winningTeam", state.getWinningTeam());

        List<Map<String, Object>> players = new ArrayList<>();
        for (Player p : state.getPlayers()) {
            Map<String, Object> pv = new LinkedHashMap<>();
            pv.put("id", p.getId());
            pv.put("name", p.getName());
            pv.put("position", p.getPosition().name());
            pv.put("team", p.getTeam());
            pv.put("trumpKeeper", p.isTrumpKeeper());
            pv.put("connected", p.isConnected());
            if (p.getId().equals(playerId)) {
                pv.put("hand", p.getHand());
            } else {
                pv.put("handCount", p.getHand().size());
            }
            players.add(pv);
        }
        view.put("players", players);

        // current trick
        List<Map<String, Object>> trickCards = new ArrayList<>();
        for (PlayedCard pc : state.getCurrentTrick().getCards()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("playerId", pc.getPlayerId());
            c.put("position", pc.getPosition().name());
            c.put("card", pc.getCard());
            c.put("trumpPlay", pc.isTrumpPlay());
            trickCards.add(c);
        }
        view.put("currentTrick", trickCards);
        view.put("tablePileCount", state.getTablePile().size());

        return view;
    }
}
