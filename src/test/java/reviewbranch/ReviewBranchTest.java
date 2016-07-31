package reviewbranch;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Test;

import reviewbranch.ReviewBranch.ReviewBranchArgs;

public class ReviewBranchTest {

  private final Git git = mock(Git.class);
  private final ReviewBoard rb = mock(ReviewBoard.class);
  private final ReviewBranchArgs args = new ReviewBranchArgs();
  private final ReviewBranch b = new ReviewBranch(git, rb, args);

  @After
  public void after() {
    verifyNoMoreInteractions(git, rb);
  }

  @Test
  public void createNewRbForOneCommit() {
    // given we want to review one new commit
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getCurrentCommitMessage()).thenReturn("commit message...");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    // when ran
    b.run();
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).getCurrentCommitMessage();
    verify(git).resetHard("commitA");
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.empty());
    verify(git).amendCurrentCommitMessage("commit message...\n\nRB=1");
  }

  @Test
  public void updateRbForOneCommit() {
    // given we want to update one commit
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getCurrentCommitMessage()).thenReturn("commit message...\nRB=1");
    // when ran
    b.run();
    // then we post an update to RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).getCurrentCommitMessage();
    verify(git).resetHard("commitA");
    verify(rb).updateRbForCurrentCommit(args, "1", Optional.empty());
  }

  @Test
  public void createNewRbForTwoCommits() {
    // given we want to review two new commits
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    when(git.getCurrentCommitMessage()).thenReturn("commit message A...", "commit message B...");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.of("1"))).thenReturn("2");
    // when ran
    b.run();
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git, atLeast(2)).getCurrentCommitMessage();
    verify(git).resetHard("commitA");
    verify(git).cherryPick("commitB");
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.of("1"));
    verify(git).amendCurrentCommitMessage("commit message A...\n\nRB=1");
    verify(git).amendCurrentCommitMessage("commit message B...\n\nRB=2");
  }

}
