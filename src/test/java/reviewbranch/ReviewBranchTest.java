package reviewbranch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Test;

public class ReviewBranchTest {

  private final Git git = mock(Git.class);
  private final ReviewBoard rb = mock(ReviewBoard.class);
  private final ReviewBranch b = new ReviewBranch(git, rb);

  @After
  public void after() {
    verifyNoMoreInteractions(git, rb);
  }

  @Test
  public void createNewRbForOneCommit() {
    // given we want to review one new commit
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getCurrentCommitMessage()).thenReturn("commit message...");
    when(rb.createNewRbForCurrentCommit()).thenReturn("1");
    // when ran
    b.run();
    // then we post a new RB for the current commit
    verify(git).getRevisionsFromOriginMaster();
    verify(git).getCurrentCommitMessage();
    verify(git).checkout("commitA");
    verify(rb).createNewRbForCurrentCommit();
    verify(git).amendCurrentCommitMessage("RB=1");
  }

  @Test
  public void updateRbForOneCommit() {
    // given we want to update one commit
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getCurrentCommitMessage()).thenReturn("commit message...\nRB=1");
    // when ran
    b.run();
    // then we post an update to RB for the current commit
    verify(git).getRevisionsFromOriginMaster();
    verify(git).getCurrentCommitMessage();
    verify(git).checkout("commitA");
    verify(rb).updateRbForCurrentCommit("1");
  }

}
