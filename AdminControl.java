import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminControl {

    private static AdminControl instance;

    public static AdminControl getInstance() {
        if (instance == null) {
            instance = new AdminControl();
        }
        return instance;
    }

    @FXML
    private TextArea getGroupID;

    @FXML
    private TextArea getUserID;

    @FXML

    // Handles user/group being added to TreeView
    private TreeView<String> showTreeView;
    private TreeItem<String> rootItem;

    // For CreationTime
    private static Map<String, Long> userCreationTimes = new HashMap<>();
    private static Map<String, Long> groupCreationTimes = new HashMap<>();

    private static Map<String, Long> lastUpdateTimes = new HashMap<>();

    public void updateLastUpdateTime(String userName, long updateTime) {
        lastUpdateTimes.put(userName, updateTime);
    }
    
    @FXML

    public void initialize() {
        rootItem = new TreeItem<>("ROOT");
        rootItem.setExpanded(true);
        showTreeView.setRoot(rootItem);
    }

    private static Map<String, List<TweetObserver>> userObservers = new HashMap<>();

    public static void registerObserver(String userName, TweetObserver observer) {
        userObservers.computeIfAbsent(userName, k -> new ArrayList<>()).add(observer);
    }

    public static void notifyObservers(String userName, String tweet) {
        List<TweetObserver> observers = userObservers.get(userName);
        if (observers != null) {
            for (TweetObserver observer : observers) {
                observer.update(userName, tweet);
            }
        }
    }
    
    private static Map<String, UserControl> userControllers = new HashMap<>();
    public static UserControl getUserController(String userName) {
        return userControllers.get(userName);
    }

    private ObservableList<String> getExistingUserIDs() {
        ObservableList<String> existingUserIDs = FXCollections.observableArrayList("user1", "user2", "user3");
        return existingUserIDs;
    }

    @FXML
    void btnAddGroup(ActionEvent event) {
        String groupName = getGroupID.getText().trim();
        if (!groupName.isEmpty()) {
            long groupCreationTime = System.currentTimeMillis();
            groupCreationTimes.put(groupName, groupCreationTime);

            TreeItem<String> groupItem = new TreeItem<>("🗐" + groupName);
            rootItem.getChildren().add(groupItem);
            getGroupID.clear();
        }
    }

    private static List<String> allUsers = new ArrayList<>();
    public static List<String> getAllUsers() {
        return allUsers;
    }

    @FXML
    void btnAddUser(ActionEvent event) {
        String userName = getUserID.getText().trim();
        if (!userName.isEmpty()) {
            // Check if the user already exists
            if (!allUsers.contains(userName)) {
                long userCreationTime = System.currentTimeMillis();
                userCreationTimes.put(userName, userCreationTime);

                TreeItem<String> userItem = new TreeItem<>("🗏" + userName);
                TreeItem<String> selectedItem = showTreeView.getSelectionModel().getSelectedItem();
    
                if (selectedItem == null || selectedItem == rootItem) {
                    rootItem.getChildren().add(userItem);
                } else {
                    selectedItem.getChildren().add(userItem);
                }
    
                getUserID.clear();
                allUsers.add(userName);
                showTreeView.refresh();
            } else {
                showPopup("Error", "User with ID '" + userName + "' already exists.");
            }
        }
    }

    // Helper Methods to count total for user/group
    private int countGroups(TreeItem<String> item) {
        int count = 0;
        if (item.getValue().startsWith("🗐")) {
            count++;
        }
        for (TreeItem<String> child : item.getChildren()) {
            count += countGroups(child);
        }
        return count;
    }
    private int countUsers(TreeItem<String> item) {
        int count = 0;

        if (item.getValue().startsWith("🗏")) {
            count++;
        }

        for (TreeItem<String> child : item.getChildren()) {
            count += countUsers(child);
        }
        return count;
    }

    // Helper method to display a popup
    private void showPopup(String title, String contentText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    @FXML
    void btnGroupTotal(ActionEvent event) {
        int groupCount = countGroups(rootItem);
        showPopup("Total Groups", "Total Groups: " + groupCount);
    }

    @FXML
    void btnUserTotal(ActionEvent event) {
        int userCount = countUsers(rootItem);
        showPopup("Total Users", "Total Users: " + userCount);
    }


    @FXML
    void btnMsgTotal(ActionEvent event) {
        int totalMessages = UserControl.getTotalMessages();
        showPopup("Total Messages", "Total Messages: " + totalMessages);
    }

    @FXML
    void btnPosPercentage(ActionEvent event) {
        int totalTweets = 0;
        int positiveTweets = 0;
    
        // Iterate through all users
        for (Map.Entry<String, UserControl.UserState> entry : UserControl.getUserStates().entrySet()) {
            UserControl.UserState userState = entry.getValue();
    
            for (String tweet : userState.getNewsFeed()) {
                if (tweet.startsWith(entry.getKey() + ":")) {
                    totalTweets++;
    
                    if (tweet.toLowerCase().contains("good") ||
                            tweet.toLowerCase().contains("great") ||
                            tweet.toLowerCase().contains("excellent")) {
                        positiveTweets++;
                    }
                }
            }
        }
    
        // Check if there are tweets to calculate the percentage
        if (totalTweets > 0) {
            double posPercentage = (double) positiveTweets / totalTweets * 100;
    
            // Format the percentage to display with two decimal places
            String formattedPercentage = String.format("%.2f", posPercentage);
            showPopup("Positive Tweets Percentage", "Percentage of Positive Tweets: " + formattedPercentage + "%");
        } else {
            showPopup("Positive Tweets Percentage", "Percentage of Positive Tweets: 0.00%");
        }
    }
    
    @FXML
    void btnUserView(ActionEvent event) {
        TreeItem<String> selectedItem = showTreeView.getSelectionModel().getSelectedItem();

        if (selectedItem != null && !selectedItem.getValue().startsWith("GROUP")) {
            try {
                String userName = selectedItem.getValue().replaceAll("[🗐🗏]", "").trim();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("UserControlPanel.fxml"));
                Parent root = loader.load();

                UserControl userController = loader.getController();
                userControllers.put(userName, userController);

                userName = selectedItem.getValue().replaceAll("[🗐🗏]", "").trim();
                Stage stage = new Stage();
                stage.setTitle("User Control Panel: " + userName); 

                userController.initData(stage, userName, getExistingUserIDs());

                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        } else {
            showPopup("Error", "Please select a user from the tree view.");
        }
    }

    @FXML
    void btnValidate(ActionEvent event) {
        // Check for duplicate IDs
        for (int i = 0; i < allUsers.size(); i++) {
            for (int j = i + 1; j < allUsers.size(); j++) {
                if (allUsers.get(i).equals(allUsers.get(j))) {
                    showPopup("Error", "All user IDs must be unique.");
                    return;
                }
            }
        }

        // Check for IDs containing spaces
        for (String userID : allUsers) {
            if (userID.contains(" ")) {
                showPopup("Error", "User IDs cannot contain spaces.");
                return;
            }
        }

        for (String userID : allUsers) {
            lastUpdateTimes.put(userID, System.currentTimeMillis());
        }

        // If both checks pass, show success popup
        showPopup("Validation", "All users validated!");
    }

    @FXML
    void btnUserCreationTime(ActionEvent event) {
        TreeItem<String> selectedItem = showTreeView.getSelectionModel().getSelectedItem();

        if (selectedItem != null && selectedItem.getValue().startsWith("🗏")) {
            String userName = selectedItem.getValue().replaceAll("[🗐🗏]", "").trim();
            long creationTime = userCreationTimes.getOrDefault(userName, 0L);

            // Show creation time in a popup
            showPopup("User Creation Time", "User '" + userName + "' was created at: " + formatDateTime(creationTime));
        } else {
            showPopup("Error", "Please select a user from the tree view.");
        }
    }

    @FXML
    void btnGroupCreationTime(ActionEvent event) {
        TreeItem<String> selectedItem = showTreeView.getSelectionModel().getSelectedItem();

        if (selectedItem != null && selectedItem.getValue().startsWith("🗐")) {
            String groupName = selectedItem.getValue().replaceAll("[🗐🗏]", "").trim();
            long creationTime = groupCreationTimes.getOrDefault(groupName, 0L);

            // Show creation time in a popup
            showPopup("Group Creation Time", "Group '" + groupName + "' was created at: " + formatDateTime(creationTime));
        } else {
            showPopup("Error", "Please select a group from the tree view.");
        }
    }

    private String formatDateTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), java.time.ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }

    @FXML
void btnLastUpdatedUser(ActionEvent event) {
    String lastUpdatedUser = findLastUpdatedUser();
    
    if (lastUpdatedUser != null) {
        showPopup("Last Updated User", "User with the latest update: " + lastUpdatedUser);
    } else {
        showPopup("Error", "No users available.");
    }
}

private String findLastUpdatedUser() {
    long latestUpdateTime = Long.MIN_VALUE;
    String lastUpdatedUser = null;

    for (Map.Entry<String, Long> entry : lastUpdateTimes.entrySet()) {
        if (entry.getValue() > latestUpdateTime) {
            latestUpdateTime = entry.getValue();
            lastUpdatedUser = entry.getKey();
        }
    }

    return lastUpdatedUser;
}
}