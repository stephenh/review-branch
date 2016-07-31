package reviewbranch;

public interface ReviewBoard {

  String createNewRbForCurrentCommit();

  void updateRbForCurrentCommit(String rbId);

}
