# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.9.4] - 2025-09-30

### Added
- New dependency management system with automatic library loading and relocation
- Caffeine cache implementation to replace Guava
- Sponge platform support with full event handling and command system
- UUID lookup functionality for player validation
- Enhanced kick checking system with scheduled task execution
- Support for Java 17 and Java 21 runtime environments
- New database metrics tracking for bStats

### Changed
- **BREAKING**: Minimum Java version upgraded from 8 to 17
- Replaced Guava cache with Caffeine cache for better performance
- Modernized player checking system with asynchronous processing
- Improved database connection handling with proper resource management
- Enhanced VPN/Proxy detection with new `CheckResult` and `ResultType` system
- Updated Maven dependencies and build process
- Reorganized project structure (Assembly → Universal module)
- Improved error handling and exception logging throughout codebase

### Fixed
- H2 database compatibility issues with automatic backup and recovery
- Memory leaks in database result set handling with try-with-resources
- Thread safety issues in player cache management
- Command registration and unregistration during plugin lifecycle
- Proper cleanup of database drivers on shutdown
- Resource management in SQL connections and prepared statements

### Removed
- Guava dependency (replaced with Caffeine and built-in utilities)
- Legacy cached response handling system
- Old table format compatibility code