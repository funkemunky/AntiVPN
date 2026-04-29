# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.10.1] - 2026-04-28

### Fixed
- Startup error on velocity instances is now corrected.

## [1.10.0] - 2026-04-07

### Added
- CIDR allowlisting, including commands to add, remove, view, and search entries
- MongoDB support for CIDR allowlist storage
- VPN detection webhooks with Discord and Slack formatting options
- Mojang API fallback support for player lookups
- Folia support

### Changed
- Improved player blocking so flagged users are removed more reliably across platforms
- Updated allowlist handling to validate CIDR entries more consistently
- Improved database cleanup for outdated cached responses

### Fixed
- SQL startup and loading issues, including MySQL library injection problems
- CIDR parsing issues and MongoDB CIDR lookup failures
- Allowlist-related SQL errors
- Repeated webhook spam from duplicate VPN detection events

### Documentation
- Expanded webhook setup documentation for Discord and Slack

## [1.9.4] - 2025-09-30

### Added
- Sponge platform support
- UUID lookup support for player validation
- Better scheduled kick checking
- Java 17 and Java 21 support
- Database metrics tracking for bStats

### Changed
- **BREAKING**: Minimum Java version upgraded from 8 to 17
- Replaced the old cache implementation with Caffeine for better performance
- Improved asynchronous player checking and VPN detection handling
- Improved database connection management and error handling

### Fixed
- H2 database compatibility issues with automatic backup and recovery
- Memory leaks and resource cleanup problems in database handling
- Thread safety issues in player cache management
- Command registration issues during plugin startup and shutdown
