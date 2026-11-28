# CLENOVA UI sprint 04 — Home

- One primary storage card replaces the large decorative gauge and duplicate storage summary. Shows actual free bytes, used bytes, percentage and total. A two-color bar shows the observed internal storage ratio; no invented recovery estimate.
- Loading, limited and unavailable data states are distinct. The bar is hidden if storage is unknown. The storage-details route retains the existing observed media/app/folder breakdown.
- Smart Scan is the primary action near the top. Existing Agent configuration and scheduler status remain separate from observed media counts. Agent activation/open behavior is unchanged.
- Quick actions reuse existing duplicate photo, large video, large file and unused app routes; no new scanner or deletion logic. Two columns on typical phones, one on narrow screens or large fonts. Existing fixed bottom navigation is unchanged.
- Home data refresh updates attached native views, rather than recreating the dashboard; scroll and touch targets remain attached. Storage detail refresh retains the existing scroll restoration.
- CI adds HomePresentation unit assertions and Home smoke checks for visible enabled primary actions at normal and compact sizes, with a real navigation round trip into Storage details.
- Runtime screenshots and physical-device visual checks remain necessary. This is an actual native source implementation, not an image mockup.
