# 1.4 - UNRELEASED

Added new `bench` option: `:ratio?` (default true); when false, the ratio column is omitted.

# 1.3 - 25 Apr 2024

The `bench` option :round-robin? now defaults to false, not true.

Bumped org.clj-commons/pretty dependency to version 2.6.0.

*Breaking Changes*

- Slight change to bench output: column titles are now centered
- The spec :net.lewisship.bench/:bind-for-args has been renamed to :bench-for-args; the previous name was incorrect

# 1.2 - 18 Mar 2024

Added support for a tagged literal that traces a form to evaluate, and the result of the evaluation.

Added the `bench-for` macro.

# 1.1 - 8 Mar 2024

Added the ability to enable tracing in specific namespaces even when the global tracing flag
is false.

Migrated to org.clj-commons/pretty.

Added new `net.lewisship.bench` namespace with useful wrappers around 
[criterium](https://github.com/hugoduncan/criterium) for benchmarking.

# v1.0 - 10 Mar 2022

Initial release.
