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
        '{"content": "발표하기", "imageUrl": "https://kiero-bucket.s3.ap-northeast-2.amazonaws.com/schedule/%E1%84%8E%E1%85%AC%E1%84%80%E1%85%B3%E1%86%AB%E1%84%8B%E1%85%A7%E1%86%BC+%E1%84%87%E1%85%A1%E1%86%AF%E1%84%91%E1%85%AD%E1%84%89%E1%85%A1%E1%84%8C%E1%85%B5%E1%86%AB.jpeg"}',
        '2026-01-23 11:05:00.000000',
        '2026-01-17 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'SCHEDULE',
        '{"content": "데모데이", "imageUrl": "https://kiero-bucket.s3.ap-northeast-2.amazonaws.com/schedule/Demoday.JPG"}',
        '2026-01-23 07:53:00.000000',
        '2026-01-17 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'COUPON',
        '{"amount": 50, "content": "놀이동산 가기"}',
        '2026-01-22 20:17:00.000000',
        '2026-01-16 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'MISSION',
        '{"amount": 35, "content": "받아쓰기 만점"}',
        '2026-01-22 19:46:00.000000',
        '2026-01-16 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'COMPLETE',
        '{"amount": 10}',
        '2026-01-22 17:15:22.000000',
        '2026-01-16 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'COMPLETE',
        '{"amount": 10}',
        '2026-01-21 20:37:00.000000',
        '2026-01-13 09:03:46.000000',
        :childId,
        :parentId
    ),
    (
        'SCHEDULE',
        '{"content": "피아노 학원", "imageUrl": "https://kiero-bucket.s3.ap-northeast-2.amazonaws.com/schedule/piano.JPG"}',
        '2026-01-21 13:55:00.000000',
        '2026-01-12 13:55:00.000000',
        :childId,
        :parentId
    );