package com.erplab.realtime;

import java.util.List;

/** Pushed to {@code /topic/changes}: the web query-key prefixes whose data changed. It carries no business data. */
public record ChangeNotice(List<String> resources) {
}
