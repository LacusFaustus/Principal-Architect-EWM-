WITH event_metrics AS (
    SELECT
        e.id AS event_id,
        e.title,
        e.category,
        e.initiator_id,
        e.created_at,
        e.published_at,
        e.event_date,
        e.participant_limit,
        e.paid,
        EXTRACT(EPOCH FROM (e.published_at - e.created_at)) / 3600 AS approval_time_hours,
        COUNT(DISTINCT r.id) AS total_requests,
        COUNT(DISTINCT CASE WHEN r.status = 'CONFIRMED' THEN r.id END) AS confirmed_participants,
        COUNT(DISTINCT CASE WHEN r.status = 'REJECTED' THEN r.id END) AS rejected_requests,
        COUNT(DISTINCT c.id) AS total_comments,
        COUNT(DISTINCT cl.id) AS total_likes,
        COUNT(DISTINCT cl.id) - COUNT(DISTINCT CASE WHEN cl.is_like = false THEN cl.id END) AS net_likes,
        SUM(s.hits) AS total_views,
        AVG(CASE WHEN r.status = 'CONFIRMED' THEN 1.0 ELSE 0.0 END) AS conversion_rate
    FROM {{ ref('events') }} e
    LEFT JOIN {{ ref('requests') }} r ON e.id = r.event_id
    LEFT JOIN {{ ref('comments') }} c ON e.id = c.event_id AND c.status = 'PUBLISHED'
    LEFT JOIN {{ ref('comment_likes') }} cl ON c.id = cl.comment_id
    LEFT JOIN {{ ref('stats') }} s ON e.id = s.event_id
WHERE e.status = 'PUBLISHED'
GROUP BY e.id, e.title, e.category, e.initiator_id, e.created_at, e.published_at, e.event_date, e.participant_limit, e.paid
    ),
    event_ranking AS (
SELECT
    *,
    ROW_NUMBER() OVER (ORDER BY confirmed_participants DESC) AS popularity_rank,
    ROW_NUMBER() OVER (ORDER BY total_views DESC) AS views_rank,
    ROW_NUMBER() OVER (ORDER BY conversion_rate DESC) AS conversion_rank,
    CASE
    WHEN confirmed_participants > 1000 THEN 'VIRAL'
    WHEN confirmed_participants > 100 THEN 'POPULAR'
    WHEN confirmed_participants > 10 THEN 'MODERATE'
    ELSE 'LOW'
    END AS popularity_tier
FROM event_metrics
    ),
    user_engagement AS (
SELECT
    u.id AS user_id,
    u.name AS user_name,
    COUNT(DISTINCT e.id) AS events_created,
    COUNT(DISTINCT r.id) AS total_participations,
    COUNT(DISTINCT c.id) AS comments_written,
    SUM(CASE WHEN e.paid = true THEN 1 ELSE 0 END) AS paid_events,
    AVG(e.confirmed_participants) AS avg_event_participants
FROM {{ ref('users') }} u
    LEFT JOIN {{ ref('events') }} e ON u.id = e.initiator_id AND e.status = 'PUBLISHED'
    LEFT JOIN {{ ref('requests') }} r ON u.id = r.requester_id AND r.status = 'CONFIRMED'
    LEFT JOIN {{ ref('comments') }} c ON u.id = c.author_id AND c.status = 'PUBLISHED'
GROUP BY u.id, u.name
    )
SELECT
    em.*,
    er.popularity_rank,
    er.views_rank,
    er.conversion_rank,
    er.popularity_tier,
    ue.user_name AS initiator_name,
    ue.total_participations AS initiator_participations,
    ue.comments_written AS initiator_comments,
    ue.paid_events AS initiator_paid_events,
    ue.avg_event_participants AS initiator_avg_participants
FROM event_metrics em
         LEFT JOIN event_ranking er ON em.event_id = er.event_id
         LEFT JOIN user_engagement ue ON em.initiator_id = ue.user_id