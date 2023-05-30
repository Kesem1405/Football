import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LeagueManager extends JPanel {

    private final int matchesPerFixture = 5;
    private List<Team> teams;
    private List<Match> matches;

    private JTextArea scoresTextArea;
    private JTextArea gamesTextArea;
    private JTextArea leagueTableTextArea;

    public LeagueManager() {
        teams = new ArrayList<>();
        matches = new ArrayList<>();
        createTeamsFromFile();
        startLeague();
    }

    private void startLeague() {
        generatePlayers();
        generateLeagueSchedule();
        playGames();
    }

    public void initializeUI() {
        setLayout(new BorderLayout());

        scoresTextArea = new JTextArea();
        scoresTextArea.setEditable(false);
        JScrollPane scoresScrollPane = new JScrollPane(scoresTextArea);
        gamesTextArea = new JTextArea();
        gamesTextArea.setEditable(false);
        JScrollPane gamesScrollPane = new JScrollPane(gamesTextArea);
        leagueTableTextArea = new JTextArea();
        leagueTableTextArea.setEditable(false);
        JScrollPane leagueTableScrollPane = new JScrollPane(leagueTableTextArea);
        add(scoresScrollPane, BorderLayout.NORTH);
        add(gamesScrollPane, BorderLayout.CENTER);
        add(leagueTableScrollPane, BorderLayout.SOUTH);
        leagueTableScrollPane.setVisible(true);
        revalidate();
        repaint();
    }


    private void createTeamsFromFile() {
        try {
            List<String> teamNames = readTeamNamesFromFile();
            List<String> firstNames = readNamesFromFile(Constants.PATH_TO_FIRST_NAMES_FILE);
            List<String> lastNames = readNamesFromFile(Constants.PATH_TO_LAST_NAMES_FILE);
            teams = createTeams(teamNames, firstNames, lastNames);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Match> findMatchesByTeam(int teamId) {
        return matches.stream().
                filter(match -> match.getHomeTeam().getId() == teamId
                        || match.getAwayTeam().getId() == teamId
                ).collect(Collectors.toList());
    }

    private List<Team> findTopScoringTeams(int n) {
        Map<Team, Integer> teamGoalsMap = teams.stream()
                .collect(Collectors.toMap(
                        team -> team,
                        team -> calculateTotalGoalsScored(team)
                ));

        return teamGoalsMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int calculateTotalGoalsScored(Team team) {
        return matches.stream()
                .filter(match -> match.getHomeTeam() == team || match.getAwayTeam() == team)
                .flatMap(match -> match.getGoals().stream())
                .mapToInt(goal -> 1)
                .sum();
    }

    private List<Player> findPlayersWithAtLeastNGoals(int n) {
        Map<Player, Integer> goalsByPlayer = matches.stream()
                .flatMap(match -> match.getGoals().stream())
                .collect(Collectors.groupingBy(Goal::getScorer, Collectors.summingInt(goal -> 1)));
        return goalsByPlayer.entrySet().stream()
                .filter(entry -> entry.getValue() >= n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Team getTeamByPosition(int position) {
        return teams.stream()
                .sorted(Comparator.comparingInt(team -> getTotalGoalsForTeam(team.getId())))
                .skip(position - 1)
                .findFirst()
                .orElse(null);
    }

    private int getTotalGoalsForTeam(int teamId) {
        return matches.stream()
                .filter(match -> match.getHomeTeam().getId() == teamId || match.getAwayTeam().getId() == teamId)
                .mapToInt(match -> match.getGoals().size())
                .sum();
    }

    private List<String> readNamesFromFile(String fileName) {
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            names = reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return names;
    }

    private List<String> readTeamNamesFromFile() {
        List<String> teamNames = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(Constants.PATH_TO_TEAMS_FILE))) {
            teamNames = reader.lines()
                    .map(line -> line.split(",")[0].trim())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return teamNames;
    }

    private void generatePlayers() {
        List<String> firstNames = readNamesFromFile(Constants.PATH_TO_FIRST_NAMES_FILE);
        List<String> lastNames = readNamesFromFile(Constants.PATH_TO_LAST_NAMES_FILE);

        Random random = new Random();

        teams.forEach(team -> {
            List<Player> players = IntStream.range(0, 15)
                    .mapToObj(i -> {
                        String firstName = firstNames.get(random.nextInt(firstNames.size()));
                        String lastName = lastNames.get(random.nextInt(lastNames.size()));
                        return new Player(i + 1, firstName, lastName);
                    })
                    .collect(Collectors.toList());
            team.setPlayers(players);
            System.out.println(team.toString());
        });
    }

    private List<Match> generateLeagueSchedule() {
        List<Match> schedule = new ArrayList<>();
        List<Team> shuffledTeams = new ArrayList<>(teams);
        Collections.shuffle(shuffledTeams);

        int numTeams = shuffledTeams.size();
        int matchesPerRound = numTeams / 2;

        IntStream.range(0, numTeams - 1).forEach(round -> {
            List<Team> roundTeams = new ArrayList<>(shuffledTeams);
            Collections.rotate(roundTeams, round);

            IntStream.range(0, matchesPerRound).forEach(match -> {
                Team homeTeam = roundTeams.get(match);
                Team awayTeam = roundTeams.get(numTeams - match - 1);
                Match newMatch = new Match(round * matchesPerRound + match + 1, homeTeam, awayTeam, new ArrayList<>());
                schedule.add(newMatch);
            });
        });

        return schedule;
    }

    public void playGames() {
        List<Match> schedule = generateLeagueSchedule();
        int fixtureCount = 0;

        while (!schedule.isEmpty()) {
            int finalFixtureCount = fixtureCount;

            List<Match> currentFixture = schedule.stream()
                    .filter(match -> match.getId() / matchesPerFixture == finalFixtureCount)
                    .sorted(Comparator.comparingInt(Match::getId)) // Sort matches by ID
                    .collect(Collectors.toList());

            List<Thread> gameThreads = currentFixture.stream()
                    .map(match -> {
                        match.setId(match.getId() + finalFixtureCount * matchesPerFixture);
                        Thread gameThread = new Thread(() -> playSingleGame(match));
                        gameThread.start();
                        return gameThread;
                    })
                    .collect(Collectors.toList());

            gameThreads.forEach(gameThread -> {
                try {
                    gameThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            updateLeagueTable();
            fixtureCount++;
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void updateScores(List<Match> matches) {
        String scores = matches.stream()
                .map(match -> {
                    Team homeTeam = match.getHomeTeam();
                    Team awayTeam = match.getAwayTeam();
                    int homeGoals = match.getHomeGoals();
                    int awayGoals = match.getAwayGoals();
                    String result;
                    if (homeGoals > awayGoals) {
                        homeTeam.setPoints(homeTeam.getPoints() + 3);
                        result = homeTeam.getName() + " wins";
                    } else if (homeGoals < awayGoals) {
                        awayTeam.setPoints(awayTeam.getPoints() + 3);
                        result = awayTeam.getName() + " wins";
                    } else {
                        homeTeam.setPoints(homeTeam.getPoints() + 1);
                        awayTeam.setPoints(awayTeam.getPoints() + 1);
                        result = "Draw";
                    }

                    return "Match " + match.getId() + ": " +
                            homeTeam.getName() + " " + homeGoals + " - " +
                            awayGoals + " " + awayTeam.getName() + " (" + result + ")";
                })
                .collect(Collectors.joining("\n"));
        scoresTextArea.setText(scores);
    }

    private void updateGames(List<Match> matches) {
        String games = matches.stream()
                .map(match -> "Match " + match.getId() + ": " +
                        match.getHomeTeam().getName() + " vs. " +
                        match.getAwayTeam().getName())
                .collect(Collectors.joining("\n"));
        gamesTextArea.setText(games);
    }


    private void updateLeagueTable() {
        if (teams == null) {
            System.out.println("Teams list is not initialized.");
            return;
        }
        String leagueTable = teams.stream()
                .sorted((t1, t2) -> Integer.compare(t2.getPoints(), t1.getPoints()))
                .map(team -> team.getName() + "\t" + team.getPoints())
                .collect(Collectors.joining("\n"));

        if (leagueTableTextArea == null) {
            System.out.println("leagueTableTextArea is not initialized.");
            return;
        }
        leagueTableTextArea.setText("League Table:\n\n" + "Team\tPoints\n" + leagueTable);
    }

    private void playSingleGame(Match match) {
        Random random = new Random();
        int homeGoals = random.nextInt(5);
        int awayGoals = random.nextInt(5);

        List<Goal> goals = Stream.concat(
                generateRandomGoals(match.getHomeTeam(), homeGoals, match.getId()),
                generateRandomGoals(match.getAwayTeam(), awayGoals, match.getId())
        ).collect(Collectors.toList());

        match.setGoals(goals);

        System.out.println("Match ID: " + match.getId());
        System.out.println("Home Team: " + match.getHomeTeam().getName() + " - Goals: " + homeGoals);
        System.out.println("Away Team: " + match.getAwayTeam().getName() + " - Goals: " + awayGoals);
        System.out.println("----------------------------");
    }

    private Stream<Goal> generateRandomGoals(Team team, int numGoals, int gameId) {
        List<Player> players = team.getPlayers();
        Random random = new Random();
        return Stream.generate(() -> generateRandomGoal(players, gameId))
                .limit(numGoals);
    }

    private Goal generateRandomGoal(List<Player> players, int gameId) {
        Random random = new Random();
        Player scorer = players.get(random.nextInt(players.size()));
        int minute = random.nextInt(90) + 1;

        return new Goal(gameId, minute, scorer);
    }


    private List<Team> createTeams(List<String> teamNames, List<String> firstNames, List<String> lastNames) {
        Random random = new Random();
        return teamNames.stream()
                .map(teamName -> {
                    List<Player> players = random.ints(1, firstNames.size())
                            .distinct()
                            .limit(Constants.NUM_PLAYERS)
                            .mapToObj(i -> new Player(i, firstNames.get(i - 1), lastNames.get(random.nextInt(lastNames.size()))))
                            .collect(Collectors.toList());
                    return new Team(teams.size() + 1, teamName, players);
                })
                .collect(Collectors.toList());
    }

    public List<Team> getTeams() {
        return this.teams;
    }
}
