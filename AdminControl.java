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
}