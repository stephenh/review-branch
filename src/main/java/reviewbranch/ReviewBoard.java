package reviewbranch;

import reviewbranch.ReviewBranch.ReviewBranchArgs;

public interface ReviewBoard {

  String createNewRbForCurrentCommit(ReviewBranchArgs args, String currentBranch);

  void updateRbForCurrentCommit(ReviewBranchArgs args, String rbId);

}
