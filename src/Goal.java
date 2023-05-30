public class Goal {
    private int id;
    private int minute;
    private Player scorer;

    public Goal(int id, int minute, Player scorer) {
        this.id = id;
        this.minute = minute;
        this.scorer = scorer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public Player getScorer() {
        return scorer;
    }

    public void setScorer(Player scorer) {
        this.scorer = scorer;
    }
}
