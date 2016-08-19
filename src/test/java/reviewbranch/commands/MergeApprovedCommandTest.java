package reviewbranch.commands;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Test;

import reviewbranch.apis.Git;
import reviewbranch.apis.ReviewBoard;

public class MergeApprovedCommandTest {

  private final Git git = mock(Git.class);
  private final ReviewBoard rb = mock(ReviewBoard.class);

  @After
  public void after() {
    verifyNoMoreInteractions(git, rb);
  }

  @Test
  public void mergeTwoApprovedCommits() {
    // given we want to merge two commits
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // and they have both been approved
    when(git.getCurrentCommitMessage()).thenReturn("CommitA.\nA=foo", "CommitB.\nA=bar");
    // when ran
    new MergeApprovedCommand().run(git, rb);
    // then we look at each commit
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getCurrentCommitMessage();
    // and merge through commitB
    verify(git).checkout("master");
    verify(git).mergeFf("commitB");
  }

  @Test
  public void mergeOnlyFirstApprovedCommit() {
    // given we want to merge three commits
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB", "commitC").toList());
    // but only the first and third have been approved
    when(git.getCurrentCommitMessage()).thenReturn("CommitA.\nA=foo", "CommitB.\nA=", "CommitC.\nA=foo");
    // when ran
    new MergeApprovedCommand().run(git, rb);
    // then we look at each commit
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git).getNote("reviewid");
    verify(git, atLeast(2)).getCurrentCommitMessage();
    // and only merge through commitA
    verify(git).checkout("master");
    verify(git).mergeFf("commitA");
  }

  @Test
  public void mergeNoCommits() {
    // given we want to merge two commits
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // but neither are approved
    when(git.getCurrentCommitMessage()).thenReturn("CommitA.\nA=", "CommitB.\nA=");
    // when ran
    new MergeApprovedCommand().run(git, rb);
    // then we look at the first commit
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getCurrentCommitMessage();
    // but bail and go back to commitB
    verify(git).resetHard("commitB");
  }


}
