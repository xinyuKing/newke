# Architecture Notes

## Layered Layout

The repository is now organized by responsibility instead of by historical arrival order.

- `apps/`: runnable entry applications such as the unified gateway and the Vue frontend.
- `services/forum/`: forum microservices.
- `services/mall/`: mall microservices.
- `shared/`: reusable shared modules split by business domain.
- `platform/`: parent POMs and dependency governance.
- `legacy/`: historical modules kept only for reference.

## Why This Matters

This layout makes it easier to answer three questions quickly:

1. Which modules are end-user entry points?
2. Which modules are business services?
3. Which modules are still historical baggage?

That reduces onboarding cost and makes future refactors less error-prone.
