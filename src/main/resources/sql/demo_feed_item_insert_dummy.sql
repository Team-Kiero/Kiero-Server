INSERT INTO feed_item (
    event_type,
    metadata,
    occurred_at,
    created_at,
    child_id,
    parent_id
)
VALUES
    (
        'SCHEDULE',
        '{"content": "피아노 학원", "imageUrl": "https://kiero-bucket.s3.ap-northeast-2.amazonaws.com/schedule/piano.JPG"}',
        '2026-01-24 11:00:00.000000',
        '2026-01-16 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'COUPON',
        '{"amount": 80, "content": "놀이동산 가기"}',
        '2026-01-24 10:00:00.000000',
        '2026-01-16 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'COMPLETE',
        '{"amount": 10}',
        '2026-01-23 17:15:22.000000',
        '2026-01-16 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'MISSION',
        '{"amount": 35, "content": "받아쓰기 만점"}',
        '2026-01-23 19:46:00.000000',
        '2026-01-16 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'SCHEDULE',
        '{"amount": 10}',
        '2026-01-22 20:37:00.000000',
        '2026-01-16 09:03:46.000000',
        :childId,
        :parentId
    );