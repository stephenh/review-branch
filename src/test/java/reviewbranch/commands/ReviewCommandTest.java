package reviewbranch.commands;

import static com.google.common.base.Charsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.Optional;

import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;

import reviewbranch.apis.Git;
import reviewbranch.apis.ReviewBoard;

@SuppressWarnings("unchecked")
public class ReviewCommandTest {

  private final Git git = mock(Git.class);
  private final ReviewBoard rb = mock(ReviewBoard.class);
  private final ReviewCommand args = new ReviewCommand();

  @After
  public void after() {
    verifyNoMoreInteractions(git, rb);
  }

  @Test
  public void createNewRbForOneCommit() {
    // given we want to review one new commit
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getNote("reviewid")).thenReturn(Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.empty());
    when(git.getCurrentDiff()).thenReturn(diffA);
    when(rb.createNewRbForCurrentCommit(args, "branch1", empty(), empty())).thenReturn("1");
    // when ran
    run();
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).getCurrentCommitMessage();
    verify(git).getCurrentDiff();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", empty(), empty());
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", Hashing.sha1().hashString(diffAWithoutIndexLine, UTF_8).toString());
  }

  @Test
  public void shouldNotCreateRbForWipCommits() {
    // given we have a commit with "wip:" prefix on its commit message, we don't want to make an RB.
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getCurrentCommitMessage()).thenReturn("wip: refactoring city");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getNote("reviewid")).thenReturn(Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.empty());
    when(git.getCurrentDiff()).thenReturn(diffA);
    when(rb.createNewRbForCurrentCommit(args, "branch1", empty(), empty())).thenReturn("1");
    // when ran
    run();
    // then we should not have a new RB
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).getCurrentCommitMessage();
    verify(git).getCurrentDiff();
    verify(rb, times(0)).createNewRbForCurrentCommit(args, "branch1", empty(), empty());
    verify(git, times(0)).setNote("reviewid", "1");
    verify(git, times(0)).setNote("reviewlasthash", Hashing.sha1().hashString(diffAWithoutIndexLine, UTF_8).toString());
  }

  @Test
  public void updateRbForOneCommit() {
    // given we want to update one commit
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"));
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of(diffAWithoutIndexLine));
    when(git.getCurrentDiff()).thenReturn(diffB);
    // when ran
    run();
    // then we post an update to RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).getCurrentCommitMessage();
    verify(git).getCurrentDiff();
    verify(rb).updateRbForCurrentCommit(args, "1", Optional.empty());
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", sha1(diffBWithoutIndexLine));
  }

  @Test
  public void createNewRbForTwoCommits() {
    // given we want to review two new commits
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // commitA
    when(git.getNote("reviewid")).thenReturn(Optional.empty(), Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.empty(), Optional.empty());
    when(git.getCurrentDiff()).thenReturn(diffA, diffB);
    when(rb.createNewRbForCurrentCommit(args, "branch1", empty(), empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", of("1"), empty())).thenReturn("2");
    // when ran
    run();
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentCommitMessage();
    verify(git, atLeast(2)).getCurrentDiff();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", empty(), empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", of("1"), empty());
    // commitA
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", sha1(diffAWithoutIndexLine));
    // commitB
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", sha1(diffBWithoutIndexLine));
  }

  @Test
  public void skipFirstRbIfItsUnchanged() {
    // given we want to review two commits
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // and the first one already has an id and unchanged tree hash
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"), Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of(sha1(diffAWithoutIndexLine)), Optional.empty());
    when(git.getCurrentDiff()).thenReturn(diffA, diffB);
    when(rb.createNewRbForCurrentCommit(args, "branch1", empty(), empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", of("1"), empty())).thenReturn("2");
    // when ran
    run();
    // then we post a new RB for the 2nd commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentCommitMessage();
    verify(git, atLeast(2)).getCurrentDiff();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", of("1"), empty());
    // commitB
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", sha1(diffBWithoutIndexLine));
  }

  @Test
  public void updateFirstRbIfItsChanged() {
    // given we want to review two commits
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // and the first one already has an id but has a new tree hash
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"), empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of(diffA), empty());
    when(git.getCurrentDiff()).thenReturn(diffA + "2", diffB);
    when(rb.createNewRbForCurrentCommit(args, "branch1", empty(), empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", of("1"), empty())).thenReturn("2");
    // when ran
    run();
    // then we post a new RB for the 2nd commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentCommitMessage();
    verify(git, atLeast(2)).getCurrentDiff();
    verify(rb).updateRbForCurrentCommit(args, "1", empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", of("1"), empty());
    // and update both commits' notes
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", sha1(diffAWithoutIndexLine + "2"));
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", sha1(diffBWithoutIndexLine));
  }

  @Test
  public void updateFirstRbIfItsRebased() {
    // given we want to review two commits
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // and the first one it's notes rebased together with another commit
    when(git.getNote("reviewid")).thenReturn(Optional.of("1\n\n3"), empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of("tree1\n\ntree3"), Optional.empty());
    when(git.getCurrentDiff()).thenReturn(diffA + "2", diffB);
    when(rb.createNewRbForCurrentCommit(args, "branch1", empty(), empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", of("1"), empty())).thenReturn("2");
    // when ran
    run();
    // then we post a new RB for the 2nd commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentCommitMessage();
    verify(git, atLeast(2)).getCurrentDiff();
    verify(rb).updateRbForCurrentCommit(args, "1", empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", of("1"), empty());
    // and update both commits' notes
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", sha1(diffAWithoutIndexLine + "2"));
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", sha1(diffBWithoutIndexLine));
  }

  @Test
  public void recoverReviewBoardFromCommitMessage() {
    // given we had a commit reverted
    when(git.getCurrentBranch()).thenReturn("branch1");
    // and have cherry picked it
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    // so we don't have it's reviewid note
    when(git.getNote("reviewid")).thenReturn(Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.empty());
    when(git.getCurrentDiff()).thenReturn(diffA);
    // but we have the reviewid in the commit message
    when(git.getCurrentCommitMessage()).thenReturn("commitA\nRB=1");
    // when ran
    run();
    // then we post a update to the RB
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).getCurrentDiff();
    verify(git).getCurrentCommitMessage();
    verify(rb).updateRbForCurrentCommit(args, "1", Optional.empty());
    // and update the commit notes
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", sha1(diffAWithoutIndexLine));
  }

  @Test
  public void setBugIdBasedOnCommitMessage() {
    // given we want to review one new commit
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getNote("reviewid")).thenReturn(Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.empty());
    when(git.getCurrentCommitMessage()).thenReturn("BUG=FOO-123");
    when(git.getCurrentDiff()).thenReturn(diffA);
    when(rb.createNewRbForCurrentCommit(args, "branch1", empty(), of("FOO-123"))).thenReturn("1");
    // when ran
    run();
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).getCurrentCommitMessage();
    verify(git).getCurrentDiff();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", empty(), of("FOO-123"));
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", Hashing.sha1().hashString(diffAWithoutIndexLine, UTF_8).toString());
  }

  private static String sha1(String diff) {
    return Hashing.sha1().hashString(diff, UTF_8).toString();
  }

  private void run() {
    args.run(git, rb);
  }

  private static final String diffA = Joiner.on("\n").join(
    "diff --git a/A.java b/A.java",
    "index 226fa5b..bd372b0 100644",
    "--- a/A.java",
    "+++ b/A.java",
    "@@ -3,6 +3,7 @@ package reviewbranch;",
    " import java.util.List;",
    " import java.util.Optional;",
    "",
    "+import org.apache.commons.lang3.StringUtils;",
    "",
    " import org.slf4j.Logger;");

  private static final String diffAWithoutIndexLine = Joiner.on("\n").join(
    "diff --git a/A.java b/A.java",
    "--- a/A.java",
    "+++ b/A.java",
    "@@ -3,6 +3,7 @@ package reviewbranch;",
    " import java.util.List;",
    " import java.util.Optional;",
    "",
    "+import org.apache.commons.lang3.StringUtils;",
    "",
    " import org.slf4j.Logger;");

  private static final String diffB = diffA.replace("A.java", "B.java");
  private static final String diffBWithoutIndexLine = diffAWithoutIndexLine.replace("A.java", "B.java");

}
