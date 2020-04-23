package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.GameLogic.gameStates.GameState;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="GAME")
public class Game {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long lobbyId;

    @Column
    private int roundsPlayed;

    @OneToMany
    private List<User> players = new ArrayList<>();

    @OneToOne
    private User currentGuesser;

    @Column
    private String currentWord;

    @ElementCollection
    private List<String> enteredClues = new ArrayList<>();

    @ElementCollection
    private List<String> words = new ArrayList<>();

    @Transient
    private GameState gameState;


    public long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    public void setRoundsPlayed(int roundsPlayed) {
        this.roundsPlayed = roundsPlayed;
    }

    public List<User> getPlayers() {
        return players;
    }

    public void addPlayer(User player) {
        this.players.add(player);
    }

    public String getCurrentWord() { return currentWord; }

    public void setCurrentWord(String currentWord) {
        this.currentWord = currentWord;
    }

    public List<String> getEnteredClues() {
        return enteredClues;
    }

    public void setEnteredClues(List<String> enteredClues) {
        this.enteredClues = enteredClues;
    }

    public void addClue(String clue){
        this.enteredClues.add(clue);
    }

    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public GameState getGameState () { return gameState; }

    public void setGameState(GameState state) { this.gameState = state; }

    public User getCurrentGuesser() {
        return currentGuesser;
    }

    public void setCurrentGuesser(User currentGuesser) {
        this.currentGuesser = currentGuesser;
    }
}
