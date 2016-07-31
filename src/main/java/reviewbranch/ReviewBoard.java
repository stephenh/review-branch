package reviewbranch;

import java.util.Optional;

import reviewbranch.ReviewBranch.ReviewBranchArgs;

public interface ReviewBoard {

  String createNewRbForCurrentCommit(ReviewBranchArgs args, String currentBranch, Optional<String> dependsOn);

  void updateRbForCurrentCommit(ReviewBranchArgs args, String rbId, Optional<String> dependsOn);

}
