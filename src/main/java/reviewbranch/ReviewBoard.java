package reviewbranch;

public interface ReviewBoard {

  String createNewRbForCurrentCommit(String currentBranch);

  void updateRbForCurrentCommit(String rbId);

}
