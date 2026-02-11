# 预测数据快速诊断 SQL 脚本

-- ============================================
-- 第 1 步：检查表是否存在
-- ============================================
SELECT
    'match_predictions' as table_name,
    EXISTS (
        SELECT FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_name = 'match_predictions'
    ) as exists;

SELECT
    'prediction_explanations' as table_name,
    EXISTS (
        SELECT FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_name = 'prediction_explanations'
    ) as exists;

-- ============================================
-- 第 2 步：检查表结构
-- ============================================
-- match_predictions 表结构
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'match_predictions'
ORDER BY ordinal_position;

-- prediction_explanations 表结构
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'prediction_explanations'
ORDER BY ordinal_position;

-- ============================================
-- 第 3 步：统计数据量
-- ============================================
SELECT
    'match_predictions' as table_name,
    COUNT(*) as total_records
FROM match_predictions;

SELECT
    'prediction_explanations' as table_name,
    COUNT(*) as total_records
FROM prediction_explanations;

-- ============================================
-- 第 4 步：查看预测数据详情
-- ============================================
-- 查看最近的预测数据（含关联比赛信息）
SELECT
    mp.match_id,
    m.match_time,
    m.status,
    ht.name as home_team,
    at.name as away_team,
    l.name as league,
    mp.model_version,
    mp.home_win_prob,
    mp.draw_prob,
    mp.away_win_prob,
    mp.confidence
FROM match_predictions mp
JOIN matches m ON mp.match_id = m.id
JOIN teams ht ON m.home_team_id = ht.id
JOIN teams at ON m.away_team_id = at.id
JOIN leagues l ON m.league_id = l.id
ORDER BY m.match_time DESC
LIMIT 10;

-- ============================================
-- 第 5 步：检查预测说明
-- ============================================
SELECT
    pe.id,
    pe.match_id,
    pe.explanation_text,
    m.match_time,
    ht.name as home_team,
    at.name as away_team
FROM prediction_explanations pe
JOIN matches m ON pe.match_id = m.id
JOIN teams ht ON m.home_team_id = ht.id
JOIN teams at ON m.away_team_id = at.id
ORDER BY m.match_time DESC
LIMIT 10;

-- ============================================
-- 第 6 步：检查即将进行的比赛是否有预测
-- ============================================
SELECT
    m.id,
    m.match_time,
    m.status,
    ht.name as home_team,
    at.name as away_team,
    CASE
        WHEN mp.match_id IS NOT NULL THEN 'YES'
        ELSE 'NO'
    END as has_prediction
FROM matches m
JOIN teams ht ON m.home_team_id = ht.id
JOIN teams at ON m.away_team_id = at.id
LEFT JOIN match_predictions mp ON m.id = mp.match_id
WHERE m.status = 'scheduled'
  AND m.match_time > NOW()
ORDER BY m.match_time
LIMIT 20;

-- ============================================
-- 第 7 步：检查 RLS 策略
-- ============================================
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual
FROM pg_policies
WHERE tablename IN ('match_predictions', 'prediction_explanations')
ORDER BY tablename, policyname;

-- ============================================
-- 第 8 步：如果没有数据，插入测试数据
-- ============================================
/*
-- 取消注释以下代码来插入测试数据

-- 首先获取一些即将进行的比赛
DO $$
DECLARE
    match_record RECORD;
BEGIN
    FOR match_record IN
        SELECT id FROM matches
        WHERE status = 'scheduled'
        AND match_time > NOW()
        ORDER BY match_time
        LIMIT 5
    LOOP
        -- 插入预测数据
        INSERT INTO match_predictions (match_id, model_version, home_win_prob, draw_prob, away_win_prob, confidence)
        VALUES (
            match_record.id,
            'v1.0',
            0.35 + (RANDOM() * 0.3),  -- 0.35-0.65
            0.20 + (RANDOM() * 0.2),  -- 0.20-0.40
            0.25 + (RANDOM() * 0.3),  -- 0.25-0.55
            0.65 + (RANDOM() * 0.25)  -- 0.65-0.90
        )
        ON CONFLICT (match_id) DO NOTHING;

        -- 插入预测说明
        INSERT INTO prediction_explanations (match_id, explanation_text)
        VALUES (
            match_record.id,
            '基于历史交锋记录、近期状态、主客场优势等因素综合分析得出的AI预测结果。'
        )
        ON CONFLICT DO NOTHING;
    END LOOP;
END $$;

-- 验证插入结果
SELECT COUNT(*) as inserted_predictions FROM match_predictions;
SELECT COUNT(*) as inserted_explanations FROM prediction_explanations;
*/

-- ============================================
-- 第 9 步：创建 RLS 策略（如果不存在）
-- ============================================
/*
-- 取消注释以下代码来创建 RLS 策略

-- 启用 RLS
ALTER TABLE match_predictions ENABLE ROW LEVEL SECURITY;
ALTER TABLE prediction_explanations ENABLE ROW LEVEL SECURITY;

-- 允许所有用户读取
DROP POLICY IF EXISTS "Enable read access for all users" ON match_predictions;
CREATE POLICY "Enable read access for all users"
    ON match_predictions
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS "Enable read access for all users" ON prediction_explanations;
CREATE POLICY "Enable read access for all users"
    ON prediction_explanations
    FOR SELECT
    USING (true);
*/
