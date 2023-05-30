import java.util.List;
import java.util.stream.Collectors;

public class Team {
    private int id;
    private String name;
    private List<Player> players;

    private int points;

    public Team(int id, String name, List<Player> players) {
        this.id = id;
        this.name = name;
        this.players = players;
    }

    public void setPoints(int points){
        this.points = points;
    }
    public int getPoints(){
        return this.points;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public String toString() {
        String playerNames = players.stream()
                .map(Player::getFullName)
                .collect(Collectors.joining("\n"));
        return "Team: " + name + " ID: " + id + " \n"
                +"Squad:" + playerNames;
    }
}
