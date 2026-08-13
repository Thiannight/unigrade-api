package com.unigrade.api.file.hash;

import com.unigrade.api.PojaGenerated;

@PojaGenerated
public record FileHash(FileHashAlgorithm algorithm, String value) {}
