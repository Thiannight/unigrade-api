package com.unigrade.api.endpoint.event.consumer.model;

import com.unigrade.api.PojaGenerated;
import com.unigrade.api.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
