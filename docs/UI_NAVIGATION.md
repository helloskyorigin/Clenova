# CLENOVA UI sprint 05 — Navigation and transitions

- Keep the established floating Home / Clean / Agent pill and separate Tools target. Preserve existing Agent conversation layout without bottom tabs while a chat is open.
- Native selected semantics and button roles, stable content descriptions, 48dp minimum touch targets, adaptive bar height for larger text and compact Tools width.
- Consolidate 14 header layouts into one balanced back/title/optional-action row. Titles wrap without overlapping controls; both protection add actions and all prior back destinations are retained. Dashboard settings and Agent header targets are also 48dp.
- Explicit tab changes use a short 140ms content-only transition, without delaying actions or animating background metadata refreshes. Reduced motion and touch exploration disable it.
- Restore Home scroll when returning from Settings or Storage details and on recreation. Existing Clean/Tools return-to-settings scroll handling remains. Explicit Home tab entry returns to its top.
- Prevent tab switching while an active Agent request or pending file/permission operation would be interrupted. No change to approval, scan, deletion or permission engines.
- CI includes pure navigation policy tests, previous regression checks, normal and compact Home assertions, and a four-tab navigation round trip. Physical-device visual checks and large-font runtime verification remain required.
