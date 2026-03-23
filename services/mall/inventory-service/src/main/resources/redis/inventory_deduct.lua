local stock = tonumber(redis.call('get', KEYS[1]))
local qty = tonumber(ARGV[1])
if not stock then
  return -1
end
if stock >= qty then
  redis.call('decrby', KEYS[1], qty)
  return 1
else
  return 0
end
