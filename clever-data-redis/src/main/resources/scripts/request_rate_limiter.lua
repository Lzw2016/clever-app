redis.replicate_commands()
local requested = 1             -- 当前请求数量
local timestamp_key = KEYS[1]   -- 最后更新时间key
local now = tonumber(ARGV[1])   -- 当前时间搓(秒)
local tokens_keys = {}          -- 令牌桶配置 [{key, times, limit}, ...]

local now_time = redis.call('TIME')
-- redis.log(redis.LOG_WARNING, "A=", now_time[1], "|B=", now_time[2])

for i = 2, #KEYS do
    local key = KEYS[i]
    local argvIndex = i * 2 - 1 -- 原始表达式: (i-1) * 2 + 1
    -- key, times, limit
    tokens_keys[i - 1] = {key, ARGV[argvIndex - 1], ARGV[argvIndex]}
end

-- redis.log(redis.LOG_WARNING, "timestamp_key=", timestamp_key)

-- 获取最后更新时间
local timestamp_value = tostring(redis.call("get", timestamp_key))
if timestamp_value == nil then
  timestamp_value = ""
end

-- redis.log(redis.LOG_WARNING, "timestamp_value=", timestamp_value)

-- 每个令牌桶的最后刷新时间
local last_refreshed_times = {}
for timestamp in string.gmatch(timestamp_value, "[^,]+") do
    if string.len(timestamp) <= 0 then
        table.insert(last_refreshed_times, 0)
    else
        table.insert(last_refreshed_times, tonumber(timestamp))
    end
end

local timestamp_key_ttl = 0         -- 最后更新时间key的过期时间
local result = {}                   -- 返回结果数组
for i = 1, #tokens_keys do
    local config = tokens_keys[i]
    local key = config[1]           -- 令牌桶key
    local times = config[2]         -- 在times秒内
    local limit = config[3]         -- 最多请求limit次
    local key_ttl = times * 2       -- 令牌桶key的过期时间
    -- redis.log(redis.LOG_WARNING, "key=", key, "|times=", times, "|limit=", limit)
    -- 当前令牌桶最后更新时间
    local last_refreshed = last_refreshed_times[i]
    if last_refreshed == nil then
        last_refreshed = 0
    end
    -- 最后更新时间key的过期时间(令牌桶key的过期时间的最大值)
    if key_ttl > timestamp_key_ttl then
        timestamp_key_ttl = key_ttl
    end
    -- 时间差
    local how_long = math.max(0, now - last_refreshed)
    local need_filled_tokens = math.floor(how_long / times) * limit;
    if need_filled_tokens >= 1 then
        last_refreshed = now - (how_long % times)
    end
    -- 令牌桶上次请求剩余令牌数量
    local last_tokens = tonumber(redis.call("get", key))
    if last_tokens == nil then
      last_tokens = limit
      last_refreshed = now
    end
    -- 令牌桶当前请求剩余令牌数量
    local filled_tokens = math.min(limit, need_filled_tokens + last_tokens)
    local limited = 1
    local new_tokens = filled_tokens
    if filled_tokens >= requested then
      new_tokens = filled_tokens - requested
      limited = 0
    end
    -- 更新令牌桶剩余令牌数量
    redis.call("setex", key, key_ttl, new_tokens)
    -- 设置返回数据项 limited, left
    result[i] = {limited, new_tokens}
    -- 更新last_refreshed
    last_refreshed_times[i] = last_refreshed
end

-- 更新最后请求时间
local timestamp_new_value = ""
for i = 1, #last_refreshed_times do
    if string.len(timestamp_new_value) <= 0 then
        timestamp_new_value = timestamp_new_value .. last_refreshed_times[i]
    else
        timestamp_new_value = timestamp_new_value .. "," .. last_refreshed_times[i]
    end
end
redis.call("setex", timestamp_key, timestamp_key_ttl, timestamp_new_value)
-- redis.log(redis.LOG_WARNING, "------------------------------------------------------------------------------------------------------------")
return result
