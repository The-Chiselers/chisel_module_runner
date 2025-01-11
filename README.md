# Chisel Module Runner

## Overview

The `Main.scala` file is a central utility designed to facilitate the generation of Verilog and synthesis of Chisel-based hardware designs. It provides a command-line interface (CLI) to dynamically load configuration classes, generate Verilog code, and run synthesis for specified modules. The `Timer` module is provided as an example to demonstrate how this utility can be used to generate Verilog and synthesize a hardware design.

This utility is particularly useful for hardware designers working with Chisel, as it automates the process of generating Verilog and running synthesis, allowing for rapid iteration and testing of hardware designs.

---

## Features

- **Dynamic Configuration Loading**: Load configuration classes at runtime to customize module behavior.
- **Verilog Generation**: Generate Verilog code for Chisel modules with customizable configurations.
- **Synthesis Support**: Run synthesis for generated Verilog using a specified technology library.
- **Command-Line Interface**: Easily control the flow of Verilog generation and synthesis via CLI commands.
- **Example Timer Module**: A fully functional `Timer` module is provided as an example to demonstrate the utility's capabilities.

---

## Usage

### Prerequisites

- Scala and SBT (Scala Build Tool) installed.
- Chisel library dependencies configured in your project.
- A technology library file (e.g., `stdcells.lib`) for synthesis.

### Running the Utility

The utility is executed via SBT with specific subcommands for Verilog generation and synthesis. Below are the available commands:

#### 1. Generate Verilog
To generate Verilog for a Chisel module, use the `verilog` subcommand. For example, to generate Verilog for the `Timer` module:

```bash
sbt "runMain tech.rocksavage.Main verilog --mode print --module tech.rocksavage.chiselware.timer.Timer --config-class tech.rocksavage.chiselware.timer.TimerConfig"
```

- `--mode`: Specifies whether to print the Verilog to the console (`print`) or write it to a file (`write`).
- `--module`: The fully qualified name of the Chisel module to generate Verilog for.
- `--config-class`: The fully qualified name of the configuration class implementing `ModuleConfig`.

#### 2. Run Synthesis
To synthesize a generated Verilog module, use the `synth` subcommand. For example, to synthesize the `Timer` module:

```bash
sbt "runMain tech.rocksavage.Main synth --module tech.rocksavage.chiselware.timer.Timer --techlib synth/stdcells.lib --config-class tech.rocksavage.chiselware.timer.TimerConfig"
```

- `--module`: The fully qualified name of the Chisel module to synthesize.
- `--techlib`: The path to the technology library file for synthesis.
- `--config-class`: The fully qualified name of the configuration class implementing `ModuleConfig`.

---

## Example: Timer Module

The `Timer` module is provided as an example to demonstrate the utility's functionality. It is a configurable timer with the following features:

- **APB Interface**: Integrates with the Advanced Peripheral Bus (APB) for memory-mapped I/O.
- **Address Decoding**: Uses the `AddrDecode` module to manage memory ranges and handle out-of-range addresses.
- **Register Map**: Utilizes a `RegisterMap` to manage addressable registers for configuration and control.

### Timer Configuration

The `TimerConfig` class defines the default configurations for the `Timer` module. These configurations are dynamically loaded by the `Main.scala` utility during Verilog generation and synthesis.

### Timer Usage

The `Timer` module can be instantiated and integrated into a larger design. Below is an example of how to instantiate and configure the `Timer` module:

```scala
val timerParams = TimerParams() // Default parameters
val timer = Module(new Timer(timerParams))
```

---

## Directory Structure

The utility generates the following directory structure during execution:

```
out/
├── synth/
│   ├── <config_name>/
│   │   ├── <module>_net.v       // Synthesized netlist
│   │   ├── log.txt              // Synthesis log
│   │   └── gates.txt            // Gate-level statistics
└── <module>_<config_name>.sv    // Generated Verilog files
```

---

## Conclusion

The `Main.scala` utility is a powerful tool for hardware designers working with Chisel. It simplifies the process of generating Verilog and running synthesis, enabling rapid development and testing of hardware designs. The provided `Timer` module serves as a practical example of how to use this utility in a real-world scenario.

For further customization, users can define their own modules and configuration classes, leveraging the utility's dynamic loading and CLI capabilities to streamline their hardware design workflow.

---

## License

This code is licensed under the Apache Software License 2.0. See [LICENSE.MD](LICENSE.MD) for details.
