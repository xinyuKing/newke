local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local algo = ARGV[3] or "fixed"
local key_type = redis.call('type', key)['ok']

if algo == "sliding" then
  if key_type ~= 'zset' and key_type ~= 'none' then
    redis.call('del', key)
  end
  local t = redis.call('time')
  local now = tonumber(t[1]) * 1000000 + tonumber(t[2])
  local window_start = now - (window * 1000000)
  redis.call('zremrangebyscore', key, 0, window_start)
  local current = redis.call('zcard', key)
  if current >= limit then
    return 0
  end
  redis.call('zadd', key, now, tostring(now))
  redis.call('expire', key, window)
  return 1
end

if key_type ~= 'string' and key_type ~= 'none' then
  redis.call('del', key)
end
local current = redis.call('incr', key)
if current == 1 then
  redis.call('expire', key, window)
end
if current > limit then
  return 0
end
return 1
