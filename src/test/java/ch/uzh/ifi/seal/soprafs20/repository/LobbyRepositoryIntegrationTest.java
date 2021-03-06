package ch.uzh.ifi.seal.soprafs20.repository;

import ch.uzh.ifi.seal.soprafs20.entity.Lobby;
import ch.uzh.ifi.seal.soprafs20.entity.Player;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LobbyRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Test
    void findByLobbyId_success() {
        Player player = new Player();
        player.setId(1L);
        player.setToken("hostToken");
        player.setUsername("Bad Bunny");

        entityManager.persist(player);
        entityManager.flush();

        Lobby lobby = new Lobby();
        lobby.setLobbyName("Ballerz");
        lobby.setHostToken("hostToken");
        lobby.setHostId(1L);
        lobby.addPlayerToLobby(player);
        lobby.setRounds(13);
        lobby.setPrivate(true);
        lobby.setCurrentNumPlayers(1);
        lobby.setCurrentNumBots(0);
        lobby.setMaxPlayersAndBots(7);

        entityManager.persist(lobby);
        entityManager.flush();

        Optional<Lobby> foundLobby = lobbyRepository.findByLobbyId(lobby.getLobbyId());
        assertTrue(foundLobby.isPresent());
        Lobby actualLobby = foundLobby.get();

        assertEquals(lobby.getLobbyId(), actualLobby.getLobbyId());
        assertEquals(lobby.getHostToken(), actualLobby.getHostToken());
        assertEquals(lobby.getHostId(), actualLobby.getHostId());
        assertTrue(actualLobby.getPlayersInLobby().contains(player));
        assertEquals(13, lobby.getRounds());
        assertTrue(lobby.isPrivate());
        assertEquals(lobby.getCurrentNumPlayers(), actualLobby.getCurrentNumPlayers());
        assertEquals(lobby.getMaxPlayersAndBots(), actualLobby.getMaxPlayersAndBots());
    }

    @Test
    void findByLobbyId_unsuccessful() {
        Lobby lobby = new Lobby();
        lobby.setLobbyName("Ballerz");
        lobby.setHostToken("hostToken");
        lobby.setHostId(1L);
        lobby.setRounds(13);
        lobby.setPrivate(true);
        lobby.setCurrentNumPlayers(1);
        lobby.setCurrentNumBots(0);
        lobby.setMaxPlayersAndBots(7);

        entityManager.persist(lobby);
        entityManager.flush();

        Optional<Lobby> foundLobby = lobbyRepository.findByLobbyId(100L);
        assertTrue(foundLobby.isEmpty());
    }

    @Test
    void findByHostId_success() {
        Player player = new Player();
        player.setId(1L);
        player.setToken("hostToken");
        player.setUsername("Bad Bunny");

        entityManager.persist(player);
        entityManager.flush();

        Lobby lobby = new Lobby();
        lobby.setLobbyName("Ballerz");
        lobby.setHostToken("hostToken");
        lobby.setHostId(1L);
        lobby.addPlayerToLobby(player);
        lobby.setRounds(20);
        lobby.setPrivate(true);
        lobby.setCurrentNumPlayers(1);
        lobby.setCurrentNumBots(0);
        lobby.setMaxPlayersAndBots(7);

        entityManager.persist(lobby);
        entityManager.flush();

        Optional<Lobby> foundLobby = lobbyRepository.findByHostId(player.getId());
        assertTrue(foundLobby.isPresent());
        Lobby actualLobby = foundLobby.get();

        assertEquals(lobby.getLobbyId(), actualLobby.getLobbyId());
        assertEquals(lobby.getHostToken(), actualLobby.getHostToken());
        assertEquals(lobby.getHostId(), actualLobby.getHostId());
        assertTrue(actualLobby.getPlayersInLobby().contains(player));
        assertEquals(13,lobby.getRounds());
        assertTrue(lobby.isPrivate());
        assertEquals(lobby.getCurrentNumPlayers(), actualLobby.getCurrentNumPlayers());
        assertEquals(lobby.getMaxPlayersAndBots(), actualLobby.getMaxPlayersAndBots());
    }

    @Test
    void findByHostId_unsuccessful() {
        Player player = new Player();
        player.setId(1L);
        player.setToken("hostToken");
        player.setUsername("Bad Bunny");

        entityManager.persist(player);
        entityManager.flush();

        Lobby lobby = new Lobby();
        lobby.setLobbyName("Ballerz");
        lobby.setHostToken("hostToken");
        lobby.setHostId(1L);
        lobby.addPlayerToLobby(player);
        lobby.setRounds(13);
        lobby.setPrivate(true);
        lobby.setCurrentNumPlayers(1);
        lobby.setCurrentNumBots(0);
        lobby.setMaxPlayersAndBots(7);

        entityManager.persist(lobby);
        entityManager.flush();

        Optional<Lobby> foundLobby = lobbyRepository.findByLobbyId(100L);
        assertTrue(foundLobby.isEmpty());
    }

}
