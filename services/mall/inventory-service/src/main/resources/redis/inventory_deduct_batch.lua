local count = #KEYS
if count == 0 then
  return -1
end

for i = 1, count do
  local stock = tonumber(redis.call('get', KEYS[i]))
  local qty = tonumber(ARGV[i])
  if not stock then
    return -1
  end
  if stock < qty then
    return 0
  end
end

for i = 1, count do
  redis.call('decrby', KEYS[i], tonumber(ARGV[i]))
end
return 1
