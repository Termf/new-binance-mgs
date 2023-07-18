local key = KEYS[1]
local len = #ARGV
for i=1, len/2 do
    redis.call('zincrby', key, ARGV[2*i], ARGV[2*i-1]);
end
return true
