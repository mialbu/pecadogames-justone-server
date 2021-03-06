package ch.uzh.ifi.seal.soprafs20.service;

import ch.uzh.ifi.seal.soprafs20.GameLogic.WordReader;
import ch.uzh.ifi.seal.soprafs20.GameLogic.GameState;
import ch.uzh.ifi.seal.soprafs20.entity.*;
import ch.uzh.ifi.seal.soprafs20.exceptions.UnauthorizedException;
import ch.uzh.ifi.seal.soprafs20.repository.*;
import ch.uzh.ifi.seal.soprafs20.rest.dto.CluePutDTO;
import ch.uzh.ifi.seal.soprafs20.rest.dto.GamePostDTO;
import ch.uzh.ifi.seal.soprafs20.rest.dto.MessagePutDTO;
import ch.uzh.ifi.seal.soprafs20.rest.dto.RequestPutDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private ClueRepository clueRepository;

    @Mock
    private LobbyScoreRepository lobbyScoreRepository;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private Lobby testLobby;
    private Player testHost;
    private Player player2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        testHost = new Player();
        testHost.setId(1L);
        testHost.setToken("hostToken");

        player2 = new Player();
        player2.setId(2L);
        player2.setToken("token2");

        testLobby = new Lobby();
        testLobby.setLobbyId(1L);
        testLobby.setHostToken("hostToken");
        testLobby.addPlayerToLobby(testHost);
        testLobby.setPrivate(false);
        testLobby.setCurrentNumPlayers(1);

        testGame = new Game();
        testGame.setLobbyId(1L);
        testGame.setRoundsPlayed(0);
        testGame.addPlayer(player2);
        testGame.addPlayer(testHost);
        testGame.setCurrentGuesser(testHost);
        testGame.setRounds(13);

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(java.util.Optional.ofNullable(testGame));
        Mockito.when(gameRepository.save(Mockito.any())).thenReturn(testGame);
    }

    @Test
    public void getGame_validInput_success() {
        Game game = gameService.getGame(testGame.getLobbyId());

        assertEquals(testGame.getLobbyId(), game.getLobbyId());
        assertEquals(testGame.getRoundsPlayed(), game.getRoundsPlayed());
        assertTrue(game.getPlayers().contains(testHost));
        assertTrue(game.getPlayers().contains(player2));
        assertEquals(testGame.getCurrentGuesser(), game.getCurrentGuesser());
    }

    @Test
    public void create_Game_validInput_success() {
        testLobby.setCurrentNumBots(0);
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setHostId(testHost.getId());
        gamePostDTO.setHostToken(testHost.getToken());

        Game game = new Game();
        game = gameService.createGame(testLobby, gamePostDTO);
        Mockito.verify(gameRepository,Mockito.times(1)).save(Mockito.any());

        assertEquals(testLobby.getLobbyId(), game.getLobbyId());
        assertTrue(game.getPlayers().contains(testHost));
        assertEquals(0, game.getRoundsPlayed());
    }

    @Test
    void sendClue_normalGame_success(){
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setSpecialGame(false);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        testGame.setCurrentWord("wars");
        testGame.setTimer(new InternalTimer());

        CluePutDTO cluePutDTO = new CluePutDTO();
        cluePutDTO.setPlayerId(player2.getId());
        cluePutDTO.setPlayerToken(player2.getToken());
        cluePutDTO.setMessage("star");

        Clue clue = new Clue();
        clue.setActualClue("star");

        gameService.sendClue(testGame, player2, cluePutDTO);

        assertTrue(testGame.getEnteredClues().contains(clue));
        assertTrue(player2.isClueIsSent());
    }

    @Test
    public void sendClue_normalGame_duplicateClue() {
        Player player3 = new Player();
        player3.setId(3L);
        player3.setToken("token3");

        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setSpecialGame(false);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        testGame.setCurrentWord("wars");
        testGame.setTimer(new InternalTimer());
        testGame.addPlayer(player3);

        CluePutDTO cluePutDTO = new CluePutDTO();
        cluePutDTO.setPlayerId(player2.getId());
        cluePutDTO.setPlayerToken(player2.getToken());
        cluePutDTO.setMessage("star");

        Clue clue = new Clue();
        clue.setActualClue("star");

        gameService.sendClue(testGame, player2, cluePutDTO);

        cluePutDTO.setPlayerToken(player3.getToken());
        cluePutDTO.setPlayerToken(player3.getToken());

        gameService.sendClue(testGame, player3, cluePutDTO);

        assertFalse(testGame.getEnteredClues().contains(clue));
        assertTrue(testGame.getInvalidClues().contains(clue));
    }

    @Test
    void sendClue_normalGame_fail_unauthorizedUser(){
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setSpecialGame(false);
        //testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

        CluePutDTO cluePutDTO = new CluePutDTO();
        cluePutDTO.setPlayerId(player2.getId());
        cluePutDTO.setPlayerToken(player2.getToken());
        cluePutDTO.setMessage("star");

        assertThrows(UnauthorizedException.class,()->{ gameService.sendClue(testGame, testHost, cluePutDTO);});
        assertTrue(testGame.getEnteredClues().isEmpty());
        assertFalse(testHost.isClueIsSent());
    }

    @Test
    public void sendClue_normalGame_playerNotInGame_unauthorized() {
        Player player2 = new Player();
        player2.setId(3L);

        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setSpecialGame(false);

        CluePutDTO cluePutDTO = new CluePutDTO();
        cluePutDTO.setPlayerId(player2.getId());
        cluePutDTO.setPlayerToken(player2.getToken());
        cluePutDTO.setMessage("star");

        assertThrows(UnauthorizedException.class,()->{gameService.sendClue(testGame, player2, cluePutDTO);});
        assertTrue(testGame.getEnteredClues().isEmpty());
    }

    @Test
    void sendClue_normalGame_invalidState() {
        testGame.setGameState(GameState.ENTER_GUESS_STATE);
        testGame.setSpecialGame(false);
        testGame.setCurrentGuesser(player2);

        CluePutDTO cluePutDTO = new CluePutDTO();
        cluePutDTO.setPlayerId(player2.getId());
        cluePutDTO.setPlayerToken(player2.getToken());
        cluePutDTO.setMessage("star");

        Exception ex = assertThrows(UnauthorizedException.class, ()->{ gameService.sendClue(testGame, testHost, cluePutDTO);});
        assertTrue(ex.getMessage().contains("not accepted in current state"));
        assertTrue(testGame.getEnteredClues().isEmpty());
        assertFalse(testHost.isClueIsSent());
    }

    @Test
    void sendClue_normalGame_clueAlreadySent() {
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setSpecialGame(false);
        testGame.setCurrentGuesser(player2);
        testHost.setClueIsSent(true);

        CluePutDTO cluePutDTO = new CluePutDTO();
        cluePutDTO.setPlayerId(player2.getId());
        cluePutDTO.setPlayerToken(player2.getToken());
        cluePutDTO.setMessage("star");

        Exception ex = assertThrows(UnauthorizedException.class, ()->{gameService.sendClue(testGame, testHost, cluePutDTO);});
        assertTrue(testGame.getEnteredClues().isEmpty());
    }

    @Test
    void sendClue_specialGame_success(){
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setSpecialGame(true);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        testGame.setCurrentWord("yoda");
        testGame.setTimer(new InternalTimer());

        Clue enteredClue1 = new Clue();
        enteredClue1.setPlayerId(2L);
        enteredClue1.setActualClue("star");

        Clue enteredClue2 = new Clue();
        enteredClue2.setPlayerId(2L);
        enteredClue2.setActualClue("wars");

        CluePutDTO cluePutDTO = new CluePutDTO();
        cluePutDTO.setPlayerId(player2.getId());
        cluePutDTO.setPlayerToken(player2.getToken());
        cluePutDTO.setMessage("star");
        cluePutDTO.setMessage2("wars");

        gameService.sendClue(testGame, player2, cluePutDTO);

        assertTrue(testGame.getEnteredClues().contains(enteredClue1));
        assertTrue(testGame.getEnteredClues().contains(enteredClue2));
        assertTrue(player2.isClueIsSent());
    }

    @Test
    void sendClue_allCluesSent_generateCluesForBots() {
        Lobby lobby = new Lobby();
        lobby.setCurrentNumBots(1);
        lobby.setLobbyId(testGame.getLobbyId());

        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setSpecialGame(false);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        testGame.setCurrentWord("tool");
        testGame.setTimer(new InternalTimer());

        CluePutDTO cluePutDTO = new CluePutDTO();
        cluePutDTO.setPlayerId(player2.getId());
        cluePutDTO.setPlayerToken(player2.getToken());
        cluePutDTO.setMessage("star");

        Clue clue = new Clue();
        clue.setActualClue("star");

        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.sendClue(testGame, player2, cluePutDTO);

        assertTrue(testGame.getEnteredClues().contains(clue));
        assertTrue(player2.isClueIsSent());
        assertEquals(2, testGame.getEnteredClues().size());
    }

    @Test
    void pickWord_validInput_success() {
        List<String> someWordAsList = new ArrayList<>();
        someWordAsList.add("Erdbeermarmeladebrot");
        testGame.setWords(someWordAsList);

        gameService.pickWord(testHost.getToken(), testGame);

        assertEquals("erdbeermarmeladebrot", testGame.getCurrentWord());
        assertEquals(GameState.ENTER_CLUES_STATE, testGame.getGameState());
    }

    @Test
    public void pickWord_unauthorizedUser() {
        List<String> someWordAsList = new ArrayList<>();
        someWordAsList.add("Erdbeermarmeladebrot");
        testGame.setWords(someWordAsList);

        assertThrows(UnauthorizedException.class,()->{ gameService.pickWord("someToken", testGame); });
    }

    @Test
    public void submitGuess_validInput_guessCorrect_success() {
        testGame.setGameState(GameState.ENTER_GUESS_STATE);
        testGame.setCurrentWord("Star Wars");
        testGame.setStartTimeSeconds(60);

        MessagePutDTO messagePutDTO = new MessagePutDTO();
        messagePutDTO.setMessage("star wars");
        messagePutDTO.setPlayerToken(testGame.getCurrentGuesser().getToken());

        gameService.submitGuess(testGame, messagePutDTO, 30);

        assertTrue(testGame.isGuessCorrect());
    }

    @Test
    void submitGuess_validInput_guessIncorrect_success() {
        testGame.setGameState(GameState.ENTER_GUESS_STATE);
        testGame.setCurrentWord("Star Wars");
        testGame.setStartTimeSeconds(60);

        MessagePutDTO messagePutDTO = new MessagePutDTO();
        messagePutDTO.setMessage("star trek");
        messagePutDTO.setPlayerToken(testGame.getCurrentGuesser().getToken());

        gameService.submitGuess(testGame, messagePutDTO, 30);

        assertFalse(testGame.isGuessCorrect());
    }

    @Test
    void submitGuess_invalidState_throwsException() {
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setCurrentWord("Star Wars");
        testGame.setStartTimeSeconds(60);

        MessagePutDTO messagePutDTO = new MessagePutDTO();
        messagePutDTO.setMessage("star wars");
        messagePutDTO.setPlayerToken(testGame.getCurrentGuesser().getToken());

        assertThrows(UnauthorizedException.class,()->{ gameService.submitGuess(testGame, messagePutDTO, 30); });
        assertFalse(testGame.isGuessCorrect());
        assertNotEquals(GameState.TRANSITION_STATE, testGame.getGameState());
    }

    @Test
    void submitGuess_invalidToken_throwsException() {
        testGame.setGameState(GameState.ENTER_GUESS_STATE);
        testGame.setCurrentWord("Star Wars");
        testGame.setStartTimeSeconds(60);

        MessagePutDTO messagePutDTO = new MessagePutDTO();
        messagePutDTO.setMessage("star wars");
        messagePutDTO.setPlayerToken("someToken");

        assertThrows(UnauthorizedException.class,()->{ gameService.submitGuess(testGame, messagePutDTO, 30); });
        assertFalse(testGame.isGuessCorrect());
        assertNotEquals(GameState.TRANSITION_STATE, testGame.getGameState());
    }

    @Test
    void allCluesSent_normalGame_returnsTrue() {
        //assume there are more than 3 players in the game
        Player player3 = new Player();
        Player player4 = new Player();
        testGame.addPlayer(player3);
        testGame.addPlayer(player4);
        testGame.setSpecialGame(false);

        assertTrue(gameService.allSent(testGame, 3));
    }

    @Test
    public void notAllCluesSent_normalGame_returnsFalse() {
        Player player3 = new Player();
        Player player4 = new Player();
        testGame.addPlayer(player3);
        testGame.addPlayer(player4);
        testGame.setSpecialGame(false);

        assertFalse(gameService.allSent(testGame, 2));
    }

    @Test
    public void allCluesSent_specialGame_returnsTrue() {
        Player player3 = new Player();
        testGame.addPlayer(player3);
        testGame.setSpecialGame(true);

        assertTrue(gameService.allSent(testGame, 2));
    }

    @Test
    public void notAllCluesSent_specialGame_returnsFalse() {
        Player player3 = new Player();
        testGame.addPlayer(player3);
        testGame.setSpecialGame(true);

        assertFalse(gameService.allSent(testGame, 1));
    }

    @Test
    void checkClues_identicalClue_removed() {
        testGame.setCurrentWord("Banana");
        Clue clue1 = new Clue();
        clue1.setActualClue("Banana");
        clue1.setPlayerId(1L);
        Clue clue2 = new Clue();
        clue2.setActualClue("Apple");
        clue2.setPlayerId(2L);
        Clue clue3 = new Clue();
        clue3.setPlayerId(3L);
        clue3.setActualClue("Banana");
        testGame.addClue(clue1);
        testGame.addClue(clue2);
        testGame.addClue(clue3);

        gameService.checkClues(testGame);

        assertFalse(testGame.getEnteredClues().contains(clue1));
        assertFalse(testGame.getEnteredClues().contains(clue3));
        assertTrue(testGame.getEnteredClues().contains(clue2));
        assertTrue(testGame.getInvalidClues().contains(clue1));

        gameService.checkVotes(testGame, 2);
    }

    @Test
    void checkVote_eliminateOneClue() {
        Clue clue1 = new Clue();
        clue1.setPlayerId(1L);
        clue1.setActualClue("Apple");
        Clue clue2 = new Clue();
        clue2.setPlayerId(2L);
        clue2.setActualClue("Banana");
        testGame.addClue(clue1);
        testGame.addClue(clue2);
        testGame.addInvalidClue(clue2);
        testGame.addInvalidClue(clue2);

        gameService.checkVotes(testGame, 2);

        assertTrue(testGame.getEnteredClues().contains(clue1));
        assertFalse(testGame.getEnteredClues().contains(clue2));
        assertEquals(1, testGame.getInvalidClues().size());
        assertTrue(testGame.getInvalidClues().contains(clue2));
    }

    @Test
    public void checkVote_eliminateNoClue() {
        Clue clue1 = new Clue();
        clue1.setPlayerId(1L);
        clue1.setActualClue("Apple");
        Clue clue2 = new Clue();
        clue2.setPlayerId(2L);
        clue2.setActualClue("Banana");
        testGame.addClue(clue1);
        testGame.addClue(clue2);

        gameService.checkVotes(testGame, 2);

        assertTrue(testGame.getEnteredClues().contains(clue1));
        assertTrue(testGame.getEnteredClues().contains(clue2));
        assertTrue(testGame.getInvalidClues().isEmpty());
    }

    @Test
    void checkVote_allCluesEliminated() {
        Clue clue1 = new Clue();
        clue1.setPlayerId(1L);
        clue1.setActualClue("banana");
        Clue clue2 = new Clue();
        clue2.setPlayerId(2L);
        clue2.setActualClue("banana");

        testGame.addClue(clue1);
        testGame.addClue(clue2);
        testGame.addInvalidClue(clue1);
        testGame.addInvalidClue(clue2);

        gameService.checkVotes(testGame, 2);

        assertEquals(1, testGame.getInvalidClues().size());
        assertTrue(testGame.getInvalidClues().contains(clue1));
    }

    @Test
    void checkInvalidClues_afterCheckVotes_oneOfTwoEliminated() {
        Clue clue1 = new Clue();
        clue1.setPlayerId(1L);
        clue1.setActualClue("Apple");
        Clue clue2 = new Clue();
        clue2.setPlayerId(2L);
        clue2.setActualClue("Banana");
        testGame.addClue(clue1);
        testGame.addClue(clue2);
        testGame.addInvalidClue(clue2);
        testGame.addInvalidClue(clue2);
        testGame.addInvalidClue(clue1);

        gameService.checkVotes(testGame, 2);

        assertEquals(1, testGame.getInvalidClues().size());
        assertTrue(testGame.getInvalidClues().contains(clue2));
        assertFalse(testGame.getInvalidClues().contains(clue1));
    }

    @Test
    void checkInvalidClues_afterCheckVotes_twoOfTwoEliminated() {
        Clue clue1 = new Clue();
        clue1.setPlayerId(1L);
        clue1.setActualClue("Apple");
        Clue clue2 = new Clue();
        clue2.setPlayerId(2L);
        clue2.setActualClue("Banana");
        testGame.addClue(clue1);
        testGame.addClue(clue2);
        testGame.addInvalidClue(clue2);
        testGame.addInvalidClue(clue2);
        testGame.addInvalidClue(clue1);
        testGame.addInvalidClue(clue1);

        gameService.checkVotes(testGame, 2);

        assertEquals(2, testGame.getInvalidClues().size());
        assertTrue(testGame.getInvalidClues().contains(clue2));
        assertTrue(testGame.getInvalidClues().contains(clue1));
    }

    @Test
    public void test_MathCeil() {
        int amountOfGuessers = 3;
        int threshold = (int)Math.ceil((float)3/2);
        int threshold2 = (int)Math.ceil((float)4/2);
        int threshold3 = (int)Math.ceil((float)5/2);

        //assertEquals(2, threshold);
        assertEquals(2, threshold);
        assertEquals(2, threshold2);
        assertEquals(3, threshold3);
    }

    @Test
    void requestLessTransitionPickWord_EnterClue() throws InterruptedException {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setLobbyName("Badbunny");
        lobby.setHostId(1L);
        lobby.setHostToken("testToken");

        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");

        Player player2 = new Player();
        player2.setId(2L);
        player1.setToken("tesToken2");


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.PICK_WORD_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(0);
        WordReader reader = new WordReader();
        testGame.setWords(reader.getRandomWords(13));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(false);
        testGame.setTime(10);
        gameService.timer(testGame);
        Thread.sleep(1000);

        assertEquals(GameState.ENTER_CLUES_STATE, testGame.getGameState());
    }

    @Test
    void requestLessTransitionEnterClue_Vote() throws InterruptedException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");

        Player player2 = new Player();
        player2.setId(2L);
        player1.setToken("tesToken2");

        Clue clue = new Clue();
        clue.setPlayerId(player1.getId());
        clue.setClueId(1L);
        clue.setActualClue("Letal");


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentWord("Banana");
        testGame.setCurrentGuesser(player2);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(0);
        testGame.addClue(clue);
        WordReader reader = new WordReader();
        testGame.setWords(reader.getRandomWords(13));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(false);
        testGame.setTime(10);
        gameService.timer(testGame);
        Thread.sleep(1000);

        assertEquals(GameState.VOTE_ON_CLUES_STATE, testGame.getGameState());
        assertTrue(player1.isClueIsSent());
    }

    @Test
    void requestLessTransitionVote_EnterGuess() throws InterruptedException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");

        Player player2 = new Player();
        player2.setId(2L);
        player1.setToken("tesToken2");

        Clue clue = new Clue();
        clue.setPlayerId(player1.getId());
        clue.setClueId(1L);
        clue.setActualClue("Letal");


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.VOTE_ON_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentWord("Banana");
        testGame.setCurrentGuesser(player2);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(0);
        testGame.addClue(clue);
        WordReader reader = new WordReader();
        testGame.setWords(reader.getRandomWords(13));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(false);
        testGame.setTime(15);
        gameService.timer(testGame);
        Thread.sleep(1000);

        assertEquals(GameState.ENTER_GUESS_STATE, testGame.getGameState());
    }

    @Test
    void requestLessTransitionEnterGuess_Transition() throws InterruptedException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");

        Player player2 = new Player();
        player2.setId(2L);
        player1.setToken("tesToken2");

        Clue clue = new Clue();
        clue.setPlayerId(player1.getId());
        clue.setClueId(1L);
        clue.setActualClue("Letal");


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_GUESS_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentWord("Banana");
        testGame.setCurrentGuesser(player2);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(0);
        testGame.addClue(clue);
        WordReader reader = new WordReader();
        testGame.setWords(reader.getRandomWords(13));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(false);
        testGame.setTime(15);
        gameService.timer(testGame);
        Thread.sleep(1000);

        assertEquals(GameState.TRANSITION_STATE, testGame.getGameState());
    }

    @Test
    public void requestLessTransitionTransition_EndGame() throws InterruptedException {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setLobbyName("Badbunny");
        lobby.setHostId(1L);
        lobby.setHostToken("testToken");


        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");

        Player player2 = new Player();
        player2.setId(2L);
        player1.setToken("tesToken2");

        Clue clue = new Clue();
        clue.setPlayerId(player1.getId());
        clue.setClueId(1L);
        clue.setActualClue("Letal");


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.END_GAME_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentWord("Banana");
        testGame.setCurrentGuesser(player2);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(0);
        testGame.addClue(clue);
        WordReader reader = new WordReader();
        testGame.setWords(reader.getRandomWords(13));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(false);

        Mockito.when(lobbyRepository.findByLobbyId(1L)).thenReturn(java.util.Optional.of(lobby));

        gameService.timer(testGame);
        Thread.sleep(1000);

        assertEquals(GameState.END_GAME_STATE, testGame.getGameState());
    }


    @Test
    void requestLessStartNewRound() throws InterruptedException {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setLobbyName("Badbunny");
        lobby.setHostId(1L);
        lobby.setHostToken("testToken");

        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        User user1 = new User();
        user1.setId(1L);
        user1.setToken("testToken");
        user1.setScore(10);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        User user2 = new User();
        user2.setId(1L);
        user2.setToken("tesToken2");
        user2.setScore(10);


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.TRANSITION_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        WordReader reader = new WordReader();
        testGame.setWords(reader.getRandomWords(13));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(false);
        testGame.setTime(5);

        Mockito.when(userRepository.findById(player1.getId())).thenReturn(java.util.Optional.of(user1));
        Mockito.when(userRepository.findById(player2.getId())).thenReturn(java.util.Optional.of(user2));


        gameService.timer(testGame);
        Thread.sleep(1000);

        assertEquals(GameState.PICK_WORD_STATE, testGame.getGameState());
    }

    @Test
    void requestLessEndGame() throws InterruptedException {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setLobbyName("Badbunny");
        lobby.setHostId(1L);
        lobby.setHostToken("testToken");

        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        User user1 = new User();
        user1.setId(1L);
        user1.setToken("testToken");
        user1.setScore(10);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        User user2 = new User();
        user2.setId(1L);
        user2.setToken("tesToken2");
        user2.setScore(10);


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.TRANSITION_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setRoundsPlayed(13);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));


        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(false);
        testGame.setTime(10);

        Mockito.when(userRepository.findById(player1.getId())).thenReturn(java.util.Optional.of(user1));
        Mockito.when(userRepository.findById(player2.getId())).thenReturn(java.util.Optional.of(user2));


        gameService.timer(testGame);
        Thread.sleep(2*1000);

        assertEquals(GameState.END_GAME_STATE, testGame.getGameState());
    }

    @Test
    void deleteGame() throws InterruptedException {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setLobbyName("Badbunny");
        lobby.setHostId(1L);
        lobby.setHostToken("testToken");
        lobby.setGameIsStarted(false);

        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        User user1 = new User();
        user1.setId(1L);
        user1.setToken("testToken");
        user1.setScore(10);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        User user2 = new User();
        user2.setId(1L);
        user2.setToken("tesToken2");
        user2.setScore(10);


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.END_GAME_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setRoundsPlayed(4);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        WordReader reader = new WordReader();
        testGame.setWords(reader.getRandomWords(13));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(false);
        testGame.setTime(10);

        Mockito.when(lobbyRepository.findByLobbyId(testGame.getLobbyId())).thenReturn(java.util.Optional.of(lobby));


        gameService.timer(testGame);
        Thread.sleep(5*1000);

        assertEquals(GameState.END_GAME_STATE, testGame.getGameState());
        assertTrue(gameRepository.findByLobbyId(testGame.getLobbyId()).isEmpty());
        assertFalse(lobby.isGameStarted());
    }

    @Test
    void userPickedWord() throws InterruptedException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        User user1 = new User();
        user1.setId(1L);
        user1.setToken("testToken");
        user1.setScore(10);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        User user2 = new User();
        user2.setId(1L);
        user2.setToken("tesToken2");
        user2.setScore(10);


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(true);
        testGame.setTime(3);

        Mockito.when(gameRepository.findByLobbyId(testGame.getLobbyId())).thenReturn(java.util.Optional.ofNullable(testGame));

        gameService.timer(testGame);
//        Thread.sleep(1*1000);

        assertEquals(GameState.ENTER_CLUES_STATE, testGame.getGameState());
    }

    @Test
    void generateCluesForBots_firstClue() throws JsonProcessingException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue = new Clue();
        clue.setActualClue("instrument");

        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setCurrentWord("tool");
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(true);
        testGame.setTime(3);

        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.generateCluesForBots(testGame);

        assertTrue(testGame.getEnteredClues().contains(clue));
    }

    @Test
    void generateCluesForBots_clueAlreadyEntered_getSecondResponse() throws JsonProcessingException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue = new Clue();
        clue.setActualClue("instrument");
        Clue clue1 = new Clue();
        clue1.setActualClue("prick");

        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setCurrentWord("tool");
        testGame.addClue(clue);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(true);
        testGame.setTime(3);

        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.generateCluesForBots(testGame);

        assertTrue(testGame.getEnteredClues().contains(clue1));
    }

    @Test
    void generateCluesForBots_twoBotsInGame_firstClues() throws JsonProcessingException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(2);

        Clue clue = new Clue();
        clue.setActualClue("instrument");
        Clue clue1 = new Clue();
        clue1.setActualClue("prick");

        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setCurrentWord("tool");
        testGame.addClue(clue);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(true);
        testGame.setTime(3);

        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.generateCluesForBots(testGame);

        assertTrue(testGame.getEnteredClues().contains(clue1));
        assertTrue(testGame.getEnteredClues().contains(clue));
    }

    @Test
    void generateCluesForBots_firstWordInvalid() throws JsonProcessingException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue = new Clue();
        clue.setActualClue("canada");


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setCurrentWord("australia");
        testGame.addClue(clue);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(true);
        testGame.setTime(3);

        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.generateCluesForBots(testGame);

        assertTrue(testGame.getEnteredClues().contains(clue));
    }

    @Test
    void generateCluesForBots_currentWordIsTwoWords() throws JsonProcessingException {
        Player player1 = new Player();
        player1.setId(1L);
        player1.setToken("testToken");
        player1.setClueIsSent(true);

        Player player2 = new Player();
        player2.setId(2L);
        player2.setToken("tesToken2");
        player2.setClueIsSent(true);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue = new Clue();
        clue.setActualClue("plants");


        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
        testGame.setCurrentGuesser(player1);
        testGame.setCurrentWord("nuclear power");
        testGame.addClue(clue);
        testGame.setRoundsPlayed(1);
        testGame.setStartTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        testGame.setTimer(new InternalTimer());
        testGame.getTimer().setCancel(true);
        testGame.setTime(3);

        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.generateCluesForBots(testGame);

        assertTrue(testGame.getEnteredClues().contains(clue));
    }

    @Test
    void updateClueScores_NormalGame_GuessCorrect() {


        Player player3 = new Player();
        player3.setId(3L);
        player3.setToken("tesToken3");
        player3.setClueIsSent(true);

        Player player4 = new Player();
        player4.setId(4L);
        player4.setToken("tesToken4");
        player4.setClueIsSent(true);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue3 = new Clue();
        clue3.setActualClue("plants");
        clue3.setClueId(1L);
        clue3.setPlayerId(player3.getId());
        clue3.setTimeNeeded(10L);


        Clue clue4 = new Clue();
        clue4.setActualClue("zombies");
        clue4.setClueId(1L);
        clue4.setPlayerId(player4.getId());
        clue4.setTimeNeeded(20L);


        player3.getClues().add(clue3);
        player4.getClues().add(clue4);

        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player3);
        testGame.addPlayer(player4);
        testGame.setCurrentGuesser(testHost);
        testGame.setCurrentWord("nuclear power");
        testGame.addClue(clue3);
        testGame.addClue(clue4);
        testGame.setRoundsPlayed(1);
        testGame.setTimer(new InternalTimer());
        testGame.setGuessCorrect(true);


        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.updateScores(testGame);

        assertEquals(20,player3.getScore());
        assertEquals(40,player4.getScore());
        assertEquals(60, testGame.getOverallScore());
    }

    @Test
    void updateClueScores_NormalGame_GuessWrong() {

        Player player3 = new Player();
        player3.setId(3L);
        player3.setToken("tesToken3");
        player3.setClueIsSent(true);
        player3.setScore(60);

        Player player4 = new Player();
        player4.setId(4L);
        player4.setToken("tesToken4");
        player4.setClueIsSent(true);
        player4.setScore(50);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue3 = new Clue();
        clue3.setActualClue("plants");
        clue3.setClueId(1L);
        clue3.setPlayerId(player3.getId());
        clue3.setTimeNeeded(10L);


        Clue clue4 = new Clue();
        clue4.setActualClue("zombies");
        clue4.setClueId(1L);
        clue4.setPlayerId(player4.getId());
        clue4.setTimeNeeded(20L);


        player3.getClues().add(clue3);
        player4.getClues().add(clue4);

        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player3);
        testGame.addPlayer(player4);
        testGame.setCurrentGuesser(testHost);
        testGame.setCurrentWord("nuclear power");
        testGame.addClue(clue3);
        testGame.addClue(clue4);
        testGame.setRoundsPlayed(1);
        testGame.setTimer(new InternalTimer());
        testGame.setOverallScore(110);
        testGame.setGuessCorrect(false);


        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.updateScores(testGame);

        assertEquals(45,player3.getScore());
        assertEquals(35,player4.getScore());
        assertEquals(player3.getScore() + player4.getScore(), testGame.getOverallScore());
    }

    @Test
    void updateClueScores_SpecialGame_GuessWrong() {

        Player player3 = new Player();
        player3.setId(3L);
        player3.setToken("tesToken3");
        player3.setClueIsSent(true);
        player3.setScore(60);

        Player player4 = new Player();
        player4.setId(4L);
        player4.setToken("tesToken4");
        player4.setClueIsSent(true);
        player4.setScore(50);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue3 = new Clue();
        clue3.setActualClue("plants");
        clue3.setClueId(1L);
        clue3.setPlayerId(player3.getId());
        clue3.setTimeNeeded(10L);


        Clue clue4 = new Clue();
        clue4.setActualClue("zombies");
        clue4.setClueId(1L);
        clue4.setPlayerId(player4.getId());
        clue4.setTimeNeeded(20L);


        player3.getClues().add(clue3);
        player4.getClues().add(clue4);

        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player3);
        testGame.addPlayer(player4);
        testGame.setCurrentGuesser(testHost);
        testGame.setCurrentWord("nuclear power");
        testGame.addClue(clue3);
        testGame.addClue(clue4);
        testGame.setRoundsPlayed(1);
        testGame.setTimer(new InternalTimer());
        testGame.setOverallScore(110);
        testGame.setSpecialGame(true);
        testGame.setGuessCorrect(false);


        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.updateScores(testGame);

        assertEquals(30,player3.getScore());
        assertEquals(20,player4.getScore());
        assertEquals(player3.getScore() + player4.getScore(), testGame.getOverallScore());
    }

    @Test
    void updateClueScores_SpecialGame_GuessCorrect() {


        Player player3 = new Player();
        player3.setId(3L);
        player3.setToken("tesToken3");
        player3.setClueIsSent(true);

        Player player4 = new Player();
        player4.setId(4L);
        player4.setToken("tesToken4");
        player4.setClueIsSent(true);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue3 = new Clue();
        clue3.setActualClue("plants");
        clue3.setClueId(1L);
        clue3.setPlayerId(player3.getId());
        clue3.setTimeNeeded(10L);


        Clue clue4 = new Clue();
        clue4.setActualClue("zombies");
        clue4.setClueId(1L);
        clue4.setPlayerId(player4.getId());
        clue4.setTimeNeeded(20L);


        player3.getClues().add(clue3);
        player4.getClues().add(clue4);

        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player3);
        testGame.addPlayer(player4);
        testGame.setCurrentGuesser(testHost);
        testGame.setCurrentWord("nuclear power");
        testGame.addClue(clue3);
        testGame.addClue(clue4);
        testGame.setRoundsPlayed(1);
        testGame.setTimer(new InternalTimer());
        testGame.setSpecialGame(true);
        testGame.setGuessCorrect(true);

        User user4 = new User();
        user4.setId(player4.getId());
        user4.setScore(player4.getScore());

        Mockito.when(userRepository.findById(player3.getId())).thenReturn(java.util.Optional.of(user4));
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.anyLong())).thenReturn(java.util.Optional.of(lobby));

        gameService.updateScores(testGame);

        assertEquals(60,player3.getScore());
        assertEquals(120,player4.getScore());
        assertEquals(player3.getScore() + player4.getScore(), testGame.getOverallScore());
    }

    @Test
    void vote(){
        Player player3 = new Player();
        player3.setId(3L);
        player3.setToken("tesToken3");
        player3.setClueIsSent(true);

        Player player4 = new Player();
        player4.setId(4L);
        player4.setToken("tesToken4");
        player4.setClueIsSent(true);
        player4.setVoted(true);

        testHost.setVoted(true);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(testGame.getLobbyId());
        lobby.setCurrentNumBots(1);

        Clue clue3 = new Clue();
        clue3.setActualClue("plants");
        clue3.setClueId(1L);
        clue3.setPlayerId(player3.getId());
        clue3.setTimeNeeded(10L);


        Clue clue4 = new Clue();
        clue4.setActualClue("zombies");
        clue4.setClueId(1L);
        clue4.setPlayerId(player4.getId());
        clue4.setTimeNeeded(20L);


        player3.getClues().add(clue3);
        player4.getClues().add(clue4);

        testGame.setLobbyId(1L);
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        testGame.setLobbyName("Test");
        testGame.addPlayer(player3);
        testGame.addPlayer(player4);
        testGame.setCurrentGuesser(testHost);
        testGame.setCurrentWord("nuclear power");
        testGame.addClue(clue3);
        testGame.addClue(clue4);
        testGame.setRoundsPlayed(1);
        testGame.setTimer(new InternalTimer());
        testGame.setSpecialGame(true);
        testGame.setGuessCorrect(true);
        List<String> invalidWords = new ArrayList<>();
        invalidWords.add("zombie");

        assertTrue(gameService.vote(testGame,player3,invalidWords));
        assertTrue(player3.isVoted());
    }

    @Test
    void getTime_PickWord(){
        testGame.setGameState(GameState.PICK_WORD_STATE);
        int time = gameService.getMaxTime(testGame);
        assertEquals(10,time);
    }
    @Test
    void getTime_EnterClue(){
        testGame.setGameState(GameState.ENTER_CLUES_STATE);
        int time = gameService.getMaxTime(testGame);
        assertEquals(30,time);
    }
    @Test
    void getTime_Vote(){
        testGame.setGameState(GameState.VOTE_ON_CLUES_STATE);
        int time = gameService.getMaxTime(testGame);
        assertEquals(15,time);
    }
    @Test
    public void getTime_Guess(){
        testGame.setGameState(GameState.ENTER_GUESS_STATE);
        int time = gameService.getMaxTime(testGame);
        assertEquals(30,time);
    }

    @Test
    void getTime_Transition(){
        testGame.setGameState(GameState.TRANSITION_STATE);
        int time = gameService.getMaxTime(testGame);
        assertEquals(5,time);
    }
    @Test
    void getTime_End(){
        testGame.setGameState(GameState.END_GAME_STATE);
        int time = gameService.getMaxTime(testGame);
        assertEquals(10,time);
    }

}
