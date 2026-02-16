# linktor

Fast, concurrent broken link checker. Available as a CLI tool and a Gradle plugin.

## CLI

### Build

```bash
./gradlew installDist
```

### Usage

```bash
./build/install/linktor/bin/linktor https://example.com
```

### Options

| Flag | Short | Description | Default |
|------|-------|-------------|---------|
| `--depth` | `-d` | Max crawl depth | 5 |
| `--concurrency` | `-c` | Max concurrent requests | 15 |
| `--timeout` | `-t` | Request timeout in ms | 5000 |
| `--check-external` | `-e` | Also check external links | false |
| `--verbose` | `-v` | Verbose output | false |
| `--max-time` | `-m` | Max total crawl time in ms (0 = unlimited) | 0 |
| `--fail-on-broken` | `-f` | Exit with code 1 if broken links found | false |

### Example

```bash
./build/install/linktor/bin/linktor https://example.com --depth 3 --check-external --fail-on-broken
```

## Gradle Plugin

Apply the plugin to any project to run link checking as a build task.

### Setup

```groovy

plugins {
    id 'io.github.linktor' version '0.1.0'
}

linktor {
    url = 'https://example.com'
    maxDepth = 3
    failOnBroken = true
}
```

### Configuration

| Property | Type | Description | Default |
|----------|------|-------------|---------|
| `url` | `String` | URL to crawl (required) | |
| `maxDepth` | `Int` | Max crawl depth | 5 |
| `concurrency` | `Int` | Max concurrent requests | 15 |
| `timeoutMs` | `Long` | Request timeout in ms | 5000 |
| `checkExternal` | `Boolean` | Also check external links | false |
| `ignorePaths` | `List<String>` | URL patterns to ignore (supports `*` wildcards) | [] |
| `maxCrawlTimeMs` | `Long` | Max total crawl time in ms (0 = unlimited) | 0 |
| `failOnBroken` | `Boolean` | Fail the build if broken links found | false |

### Run

```bash
./gradlew checkLinks
```

## Development

Requires JDK 17+.

```bash
./gradlew build    # compile + test
./gradlew test     # tests only
```
