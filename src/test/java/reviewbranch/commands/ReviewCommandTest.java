package reviewbranch.commands;

import static com.google.common.base.Charsets.UTF_8;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    // when ran
    run();
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).getCurrentDiff();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.empty());
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", Hashing.sha1().hashString(diffAWithoutIndexLine, UTF_8).toString());
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
    verify(git).getCurrentDiff();
    verify(rb).updateRbForCurrentCommit(args, "1", Optional.empty());
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
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.of("1"))).thenReturn("2");
    // when ran
    run();
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentDiff();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.of("1"));
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
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.of("1"))).thenReturn("2");
    // when ran
    run();
    // then we post a new RB for the 2nd commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentDiff();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.of("1"));
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
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"), Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of(diffA), Optional.empty());
    when(git.getCurrentDiff()).thenReturn(diffA + "2", diffB);
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.of("1"))).thenReturn("2");
    // when ran
    run();
    // then we post a new RB for the 2nd commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentDiff();
    verify(rb).updateRbForCurrentCommit(args, "1", Optional.empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.of("1"));
    // and update both commits' notes
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
    when(git.getNote("reviewid")).thenReturn(Optional.of("1\n\n3"), Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of("tree1\n\ntree3"), Optional.empty());
    when(git.getCurrentDiff()).thenReturn(diffA + "2", diffB);
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.of("1"))).thenReturn("2");
    // when ran
    run();
    // then we post a new RB for the 2nd commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).resetHard("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentDiff();
    verify(rb).updateRbForCurrentCommit(args, "1", Optional.empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.of("1"));
    // and update both commits' notes
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", sha1(diffAWithoutIndexLine + "2"));
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", sha1(diffBWithoutIndexLine));
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
