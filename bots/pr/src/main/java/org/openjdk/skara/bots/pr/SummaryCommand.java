/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.skara.bots.pr;

import org.openjdk.skara.forge.PullRequest;
import org.openjdk.skara.issuetracker.Comment;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SummaryCommand implements CommandHandler {
    private static final Pattern INVALID_SUMMARY_PATTERN = Pattern.compile("(^(Co-authored-by:)(.*))|(^(Reviewed-by:)(.*))|(^(Backport-of:)(.*))|(^[0-9]+:(.*))");
    @Override
    public void handle(PullRequestBot bot, PullRequest pr, CensusInstance censusInstance, Path scratchPath, CommandInvocation command, List<Comment> allComments, PrintWriter reply) {
        if (!command.user().equals(pr.author())) {
            reply.println("Only the author (@" + pr.author().username() + ") is allowed to issue the `/summary` command.");
            return;
        }

        var currentSummary = Summary.summary(pr.repository().forge().currentUser(), allComments);
        if (command.args().isBlank()) {
            if (currentSummary.isPresent()) {
                reply.println("Removing existing summary");
                reply.println(Summary.setSummaryMarker(""));
            } else {
                reply.println("To set a summary, use the syntax `/summary <summary text>`");
            }
        } else {
            var summary = command.args().lines()
                                 .map(String::strip)
                                 .collect(Collectors.joining("\n"));
            if (!checkSummary(summary)) {
                reply.println("Invalid summary:\n" +
                        "\n" +
                        "```\n" +
                        summary +
                        "\n```\n" +
                        "A summary line cannot start with any of the following: " +
                        "`<issue-id>:`, `Co-authored-by:`, `Reviewed-by:`, `Backport-of:`. " +
                        "See [JEP 357](https://openjdk.org/jeps/357) for details.");
            } else {
                var action = currentSummary.isPresent() ? "Updating existing" : "Setting";
                if (summary.contains("\n")) {
                    reply.println(action + " summary to:\n" +
                            "\n" +
                            "```\n" +
                            summary +
                            "\n```");
                } else {
                    reply.println(action + " summary to `" + summary + "`");
                }
                reply.println(Summary.setSummaryMarker(summary));
            }
        }
    }

    @Override
    public String description() {
        return "updates the summary in the commit message";
    }

    @Override
    public boolean multiLine() {
        return true;
    }

    @Override
    public boolean allowedInBody() {
        return true;
    }

    private boolean checkSummary(String summary) {
        String[] lines = summary.split("\n");
        for (String line : lines) {
            if (INVALID_SUMMARY_PATTERN.matcher(line).matches()) {
                return false;
            }
        }
        return true;
    }
}
