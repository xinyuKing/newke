$forumModules = @(
  "services/forum/community-user-service",
  "services/forum/community-message-service",
  "services/forum/community-social-service",
  "services/forum/community-media-service",
  "services/forum/community-data-service",
  "services/forum/community-post-service"
)

$mallModules = @(
  "services/mall/auth-service",
  "services/mall/product-service",
  "services/mall/inventory-service",
  "services/mall/order-service",
  "services/mall/cart-service",
  "services/mall/support-service"
)

Write-Output "Forum startup order:"
$forumModules | ForEach-Object { Write-Output (" - " + $_) }
Write-Output ""
Write-Output "Mall startup order:"
$mallModules | ForEach-Object { Write-Output (" - " + $_) }
Write-Output ""
Write-Output "Then start:"
Write-Output " - apps/gateway-service"
Write-Output " - apps/frontend"
