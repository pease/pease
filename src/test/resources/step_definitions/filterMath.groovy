import pease.support.TableTestWorld

World(TableTestWorld.class)

Given(~/I have following users/) { table ->
  users = table
}

When(~/I filter those from Germany/) {
  users = filterTable(users)
}

Then(~/I should have the following users/) { table ->
  users == table
}