import { createRouter, createWebHistory } from "vue-router";

import Home from "../modules/forum/views/Home.vue";
import PostDetail from "../modules/forum/views/PostDetail.vue";
import Login from "../modules/forum/views/Login.vue";
import Register from "../modules/forum/views/Register.vue";
import Settings from "../modules/forum/views/Settings.vue";
import Profile from "../modules/forum/views/Profile.vue";
import Search from "../modules/forum/views/Search.vue";
import Messages from "../modules/forum/views/Messages.vue";
import MessageDetail from "../modules/forum/views/MessageDetail.vue";
import Notices from "../modules/forum/views/Notices.vue";
import NoticeDetail from "../modules/forum/views/NoticeDetail.vue";
import Followees from "../modules/forum/views/Followees.vue";
import Followers from "../modules/forum/views/Followers.vue";
import MyPosts from "../modules/forum/views/MyPosts.vue";
import MyReplies from "../modules/forum/views/MyReplies.vue";
import AdminData from "../modules/forum/views/AdminData.vue";
import MallHome from "../modules/mall/views/MallHome.vue";
import MallProductDetail from "../modules/mall/views/MallProductDetail.vue";
import MallCart from "../modules/mall/views/MallCart.vue";
import MallOrders from "../modules/mall/views/MallOrders.vue";
import MallOrderDetail from "../modules/mall/views/MallOrderDetail.vue";
import MallAccount from "../modules/mall/views/MallAccount.vue";
import MallSupport from "../modules/mall/views/MallSupport.vue";
import NotFound from "../modules/shared/views/NotFound.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: Home },
    { path: "/post/:id", component: PostDetail },
    { path: "/login", component: Login },
    { path: "/register", component: Register },
    { path: "/settings", component: Settings },
    { path: "/profile/:id", component: Profile },
    { path: "/search", component: Search },
    { path: "/messages", component: Messages },
    { path: "/messages/:conversationId", component: MessageDetail },
    { path: "/notices", component: Notices },
    { path: "/notices/:topic", component: NoticeDetail },
    { path: "/followees/:id", component: Followees },
    { path: "/followers/:id", component: Followers },
    { path: "/myposts/:id", component: MyPosts },
    { path: "/myreplies/:id", component: MyReplies },
    { path: "/admin/data", component: AdminData },
    { path: "/shop", redirect: "/mall" },
    { path: "/mall", component: MallHome },
    { path: "/mall/products/:id", component: MallProductDetail },
    { path: "/mall/cart", component: MallCart },
    { path: "/mall/orders", component: MallOrders },
    { path: "/mall/orders/:orderNo", component: MallOrderDetail },
    { path: "/mall/account", component: MallAccount },
    { path: "/mall/support", component: MallSupport },
    { path: "/:pathMatch(.*)*", component: NotFound }
  ]
});

export default router;
