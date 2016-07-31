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
    // when ran
    b.run();
    // then we post a new RB for the current commit
    verify(git).getRevisionsFromOriginMaster();
    verify(git).checkout("commitA");
    verify(rb).createNewRbForCurrentCommit();
  }

}
