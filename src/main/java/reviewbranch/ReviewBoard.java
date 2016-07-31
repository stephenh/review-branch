package reviewbranch;

public interface ReviewBoard {

  void createNewRbForCurrentCommit();

  void updateRbForCurrentCommit(String rbId);

}
