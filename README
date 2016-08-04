
review-branch
=============

`review-branch` is a script to turn 1 local branch into multiple ReviewBoard code reviews.

It currently assumes an internal environment, because instead of running the `rbt` tool, it uses an internal `git review` command, although in theory it could be adapted to use the raw `rbt` command itself; using `git review` was just the shortest path for me.

Usage
=====

On your local branch, create code reviews by running:

    review-branch review [--reviewers bob,fred] [--groups my-reviewers] [--publish] [--testing-done "yep!"]

After you get ship it's, amend your commits:

    review-branch dcommit

Then merge your branch to master (e.g. `git merge --ff local-branch`) and `git push`.

Install from source
===================

1. Clone this repo
2. Run `gradle shadowJar`
3. Copy `build/libs/review-branch-all.jar` and `review-branch` to your home directory (or other misc tools directory)
4. `chmod u+x ~/review-branch`
5. Now run `~/review-branch review` or `~/review-branch dcommit` in your project's directory

Install from pre-built jar
==========================

1. Download [review-branch-all.jar](http://repo.joist.ws/review-branch-all.jar) and [review-branch](http://repo.joist.ws/review-branch) to your home directory (or other misc tools directory)
2. `chmod u+x review-branch`
3. Now run `~/review-branch review` or `~/review-branch dcommit` in your project's directory

