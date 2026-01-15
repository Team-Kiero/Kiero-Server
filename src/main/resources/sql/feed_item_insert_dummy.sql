INSERT INTO feed_item (
    id,
    event_type,
    metadata,
    occurred_at,
    created_at,
    child_id,
    parent_id
)
VALUES
    (1,'MISSION','{"amount": 100, "content": "미션 1"}','2026-01-10 04:09:10.308177','2026-01-10 04:09:10.330633',:childId,:parentId),
    (2,'SCHEDULE','{"content": "과자먹기", "imageUrl": "aaa.com"}','2026-01-10 04:12:33.377100','2026-01-10 04:12:33.381056',:childId,:parentId);




