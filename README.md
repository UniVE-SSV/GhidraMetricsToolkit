# Ghidra Metrics Toolkit

Ghidra Metrics Toolkit is a Ghidra plugin that provides a collection of Software Metrics.

All the metrics can be computed through the GUI as well as through the script, even in headless mode.

![Screenshot](https://github.com/user-attachments/assets/6fc70026-b222-42cc-9728-ce671166c3ad)


## Installing

Download the latest release matching your Ghidra version and install it through the `File > Install Extensions` menu in the Ghidra tool launcher, or unzip the content in the `<GhidraInstallDir>/Ghidra/Extensions` folder.

The ROP Gadget Survival Similarity metric relies on `ROPGadget`, which must be installed with the `pip install ROPGadget`.

## Running

The Ghidra Metrics Toolkit GUI can be opened through the `Window > Ghidra Metrics ToolkitPlugin` menu in the CodeBrowser.

## Metrics

### McCabe Cyclomatic Complexity

This plugin provides a method to compute the overall McCabe Cyclomatic Complexity of the entire program, in addition to Ghidra's own `ghidra.program.util.CyclomaticComplexity` that computes it on a per-function basis.

Headless Parameters:

- `--overall-only`: computes only the overall complexity.

### Entropy

The entropy is computed on the overall binary as well as on each program section. The base is customizable and defaulted to 2.

Headless Parameters:

- `--base <int>`: sets a base value for the computation. 

### Halstead Metrics

Halstead Metrics are provided both for the overall program and the currently highlighted function in the code listing. When using the GUI, both are computed automatically on every program or function change, while the headless script only computes it for the whole program.

### Similarity

The plugin provides a Binary Similarity tool that computes an overall similarity score and matches each function in a program with the most similar in the second.

Several function level similarity metrics are provided:

- Longest Common Subsequence
- Normalized Compression Distance
- Opcode Frequency Histogram Matching
- Levenshtein Distance
- Jaro-Winkler Similarity
- Jaccard Index

The Normalized Compression Distance metric uses the built-in `lrzip` binary to compress the programs, and is therefore only available on linux x86-64 and arm64 platforms.

Additionally, the tool allows to customize the matching strategy with 3 options:

- Exclusive: whether matches should be one-to-one or one-to-many;
- Weighted: whether the overall similarity score should depend on the weight of the functions (in terms of number of instructions);
- Symmetric: whether the overall similarity score should be computed by averaging $S(P_1, P_2)$ and $S(P_2, P_1)$.

Headless Parameters:

- `programName`: the name of the second program for the comparison. Must be part of the Ghidra project;
- `--binary-only`: only computes the overall similarity (only for NCD);
- `--exclusive`;
- `--weighted`;
- `--symmetric`.

### ROP Gadget Survival

This metric computes the percentage of gadgets present in the first program that survive in the second program. More specifically, two variants are computed:

- Survivor: considers both the sequence of bytes of a gadget and its program offset;
- Bag of Gadgets: only considers the sequence of bytes, regardless of its position.

It is possible to customize the maximum ROP depth search size in bytes, defaulted to 10.

Headless Parameters:
- `--depth <int>`: sets the search depth.

## Exporting

To export data, use the `--csv-export <path>` option in headless mode, with any metric. Appending to the same file in multiple runs is allowed.
