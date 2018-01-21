/*
 * Jenkins Plugin for SonarQube, open source software quality management tool.
 * mailto:contact AT sonarsource DOT com
 *
 * Jenkins Plugin for SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Jenkins Plugin for SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.fortify.integration.jenkins.ssc.action;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;

@SuppressWarnings("rawtypes")
@Extension
/**
 * We don't use {@link TransientProjectActionFactory} because it appears to be cached and requires Jenkins to restart.
 */
public class FortifySSCActionFactory extends TransientActionFactory<Job> {
  @Override
  public Class<Job> type() {
    return Job.class;
  }

  @Override
  public Collection<? extends Action> createFor(Job job) {
    List<FortifySSCPageAction> actions = new LinkedList<>();

    // don't fetch builds that haven't finished yet
    Run<?, ?> lastBuild = job.getLastCompletedBuild();

    if (lastBuild != null) { 
      // TODO Don't include the action if the job no longer has the FortifySSCJenkinsBuilder/PublishResultsToJenkins step,
      //      even if the last build had this step enabled
      for (FortifySSCPublishAction a : lastBuild.getActions(FortifySSCPublishAction.class)) {
        actions.add(new FortifySSCPageAction(a));
      }
    }

    return actions;
  }
}
