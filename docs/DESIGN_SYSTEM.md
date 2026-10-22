# ShoutSend Design System

## Overview
ShoutSend uses a centralized design-system token architecture conforming to Material You (Material 3) principles.

## Structure (To be implemented in Step 2)
- `ColorTokens`: Dynamic Material 3 color roles.
- `ShapeTokens`: Corner radii tokens.
- `TypographyTokens`: Type scale.
- `SpacingTokens`: Spacing hierarchy.
- `DefaultTokens.kt`: The single file containing factory-default seed values.

All UI screens must consume tokens only. No hardcoded hex or radii values allowed.
