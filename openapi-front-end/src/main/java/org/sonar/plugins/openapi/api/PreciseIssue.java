/*
 * SonarQube OpenAPI Plugin
 * Copyright (C) 2018-2019 Societe Generale
 * vincent.girard-reydet AT socgen DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.openapi.api;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.sonar.sslr.yaml.grammar.JsonNode;

public class PreciseIssue {

  private final IssueLocation primaryLocation;
  private final List<IssueLocation> secondaryLocations;
  private Integer cost;

  public PreciseIssue(IssueLocation primaryLocation) {
    this.primaryLocation = primaryLocation;
    this.secondaryLocations = new ArrayList<>();
  }

  @Nullable
  public Integer cost() {
    return cost;
  }

  public PreciseIssue withCost(int cost) {
    this.cost = cost;
    return this;
  }

  public IssueLocation primaryLocation() {
    return primaryLocation;
  }

  public PreciseIssue secondary(JsonNode node, @Nullable String message) {
    secondaryLocations.add(IssueLocation.preciseLocation(message, node));
    return this;
  }

  public PreciseIssue secondary(IssueLocation issueLocation) {
    secondaryLocations.add(issueLocation);
    return this;
  }

  public List<IssueLocation> secondaryLocations() {
    return secondaryLocations;
  }

  @Override
  public boolean equals(Object o){
    if (o instanceof PreciseIssue){
      PreciseIssue oCasted = (PreciseIssue) o;
      boolean equals = oCasted.cost == this.cost;
      if (this.primaryLocation != null){
        equals &= this.primaryLocation.equals(oCasted.primaryLocation());
      }else{
        equals &= (oCasted.primaryLocation == null);
      }
      if (this.secondaryLocations != null){
        equals &= this.secondaryLocations.equals(oCasted.secondaryLocations());
      }else{
        equals &= (oCasted.secondaryLocations == null);
      }
      return  equals;
    }
    return false;
  }

  @Override
  public int hashCode(){
    int hashCode = 0;
    if (cost != null){
      hashCode +=cost;
    }
    if (primaryLocation != null){
      hashCode+=primaryLocation.hashCode();
    }
    if (secondaryLocations != null){
      hashCode+=secondaryLocations.hashCode();
    }
    return  hashCode;
  }

}
