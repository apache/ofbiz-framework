<nav x-data="{ open: false }" class="bg-gray-800">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="flex items-center justify-between h-16">
      <div class="flex items-center">
        <div class="flex-shrink-0">
          <div class="rounded-md bg-white p-1">
          <img class="h-8 w-8" src="https://uilogos.co/img/logomark/lighting.png"
               alt="Workflow">
          </div>
        </div>
        <div class="hidden md:block">
          <div class="ml-10 flex items-baseline space-x-4">

            <a href="<@ofbizUrl>main</@ofbizUrl>" class="bg-gray-900 text-white  px-3 py-2 rounded-md text-sm font-medium"
               aria-current="page" x-state:on="Current" x-state:off="Default"
               x-state-description="Current: &quot;bg-gray-900 text-white&quot;, Default: &quot;text-gray-300 hover:bg-gray-700 hover:text-white&quot;">Home</a>

            <a href="<@ofbizUrl>settings</@ofbizUrl>"
               class="text-gray-300 hover:bg-gray-700 hover:text-white  px-3 py-2 rounded-md text-sm font-medium"
               x-state-description="undefined: &quot;bg-gray-900 text-white&quot;, undefined: &quot;text-gray-300 hover:bg-gray-700 hover:text-white&quot;">Settings</a>

            <a href="/webtools?externalLoginKey=${externalLoginKey}"
               class="text-gray-300 hover:bg-gray-700 hover:text-white  px-3 py-2 rounded-md text-sm font-medium"
               x-state-description="undefined: &quot;bg-gray-900 text-white&quot;, undefined: &quot;text-gray-300 hover:bg-gray-700 hover:text-white&quot;">Webtools</a>

          </div>
        </div>
      </div>
      <div class="hidden md:block">
        <div class="ml-4 flex items-center md:ml-6">
          <!-- Profile dropdown -->
          <div x-data="Components.menu({ open: false })" x-init="init()"
               @keydown.escape.stop="open = false; focusButton()"
               @click.away="onClickAway($event)" class="ml-3 relative">
            <div>
              <button type="button"
                      class="max-w-xs bg-gray-800 rounded-full flex items-center text-sm focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-white"
                      id="user-menu-button" x-ref="button" @click="onButtonClick()"
                      @keyup.space.prevent="onButtonEnter()"
                      @keydown.enter.prevent="onButtonEnter()" aria-expanded="false"
                      aria-haspopup="true" x-bind:aria-expanded="open.toString()"
                      @keydown.arrow-up.prevent="onArrowUp()"
                      @keydown.arrow-down.prevent="onArrowDown()">
                <span class="sr-only">Open user menu</span>
                <img class="h-8 w-8 rounded-full"
                     src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&amp;ixid=eyJhcHBfaWQiOjEyMDd9&amp;auto=format&amp;fit=facearea&amp;facepad=2&amp;w=256&amp;h=256&amp;q=80"
                     alt="">
              </button>
            </div>

            <div x-show="open" x-transition:enter="transition ease-out duration-100"
                 x-transition:enter-start="transform opacity-0 scale-95"
                 x-transition:enter-end="transform opacity-100 scale-100"
                 x-transition:leave="transition ease-in duration-75"
                 x-transition:leave-start="transform opacity-100 scale-100"
                 x-transition:leave-end="transform opacity-0 scale-95"
                 class="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg py-1 bg-white ring-1 ring-black ring-opacity-5 focus:outline-none"
                 x-ref="menu-items" x-description="Dropdown menu, show/hide based on menu state."
                 x-bind:aria-activedescendant="activeDescendant" role="menu"
                 aria-orientation="vertical" aria-labelledby="user-menu-button" tabindex="-1"
                 @keydown.arrow-up.prevent="onArrowUp()"
                 @keydown.arrow-down.prevent="onArrowDown()" @keydown.tab="open = false"
                 @keydown.enter.prevent="open = false; focusButton()"
                 @keyup.space.prevent="open = false; focusButton()" style="display: none;">

<#--
              <a href="#" class="block px-4 py-2 text-sm text-gray-700" x-state:on="Active"
                 x-state:off="Not Active" :class="{ 'bg-gray-100': activeIndex === 0 }"
                 role="menuitem" tabindex="-1" id="user-menu-item-0" @mouseenter="activeIndex = 0"
                 @mouseleave="activeIndex = -1" @click="open = false; focusButton()">Your
                Profile</a>

              <a href="#" class="block px-4 py-2 text-sm text-gray-700"
                 :class="{ 'bg-gray-100': activeIndex === 1 }" role="menuitem" tabindex="-1"
                 id="user-menu-item-1" @mouseenter="activeIndex = 1"
                 @mouseleave="activeIndex = -1" @click="open = false; focusButton()">Settings</a>
-->

              <a href="<@ofbizUrl>logout</@ofbizUrl>"
                 class="block px-4 py-2 text-sm text-gray-700"
                 :class="{ 'bg-gray-100': activeIndex === 2 }" role="menuitem" tabindex="-1"
                 id="user-menu-item-2" @mouseenter="activeIndex = 2"
                 @mouseleave="activeIndex = -1" @click="open = false; focusButton()">Sign out</a>

            </div>

          </div>
        </div>
      </div>
      <div class="-mr-2 flex md:hidden">
        <!-- Mobile menu button -->
        <button type="button"
                class="bg-gray-800 inline-flex items-center justify-center p-2 rounded-md text-gray-400 hover:text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-white"
                aria-controls="mobile-menu" @click="open = !open" aria-expanded="false"
                x-bind:aria-expanded="open.toString()">
          <span class="sr-only">Open main menu</span>
          <svg x-state:on="Menu open" x-state:off="Menu closed" class="h-6 w-6 block"
               :class="{ 'hidden': open, 'block': !(open) }"
               x-description="Heroicon name: outline/menu" xmlns="http://www.w3.org/2000/svg"
               fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M4 6h16M4 12h16M4 18h16"></path>
          </svg>
          <svg x-state:on="Menu open" x-state:off="Menu closed" class="h-6 w-6 hidden"
               :class="{ 'block': open, 'hidden': !(open) }"
               x-description="Heroicon name: outline/x" xmlns="http://www.w3.org/2000/svg"
               fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>
    </div>
  </div>

  <div x-description="Mobile menu, show/hide based on menu state." class="md:hidden"
       id="mobile-menu" x-show="open" style="display: none;">
    <div class="px-2 pt-2 pb-3 space-y-1 sm:px-3">

      <a href="<@ofbizUrl>main</@ofbizUrl>" class="bg-gray-900 text-white  block px-3 py-2 rounded-md text-base font-medium"
         aria-current="page" x-state:on="Current" x-state:off="Default"
         x-state-description="Current: &quot;bg-gray-900 text-white&quot;, Default: &quot;text-gray-300 hover:bg-gray-700 hover:text-white&quot;">Home</a>

      <a href="<@ofbizUrl>settings</@ofbizUrl>"
         class="text-gray-300 hover:bg-gray-700 hover:text-white  block px-3 py-2 rounded-md text-base font-medium"
         x-state-description="undefined: &quot;bg-gray-900 text-white&quot;, undefined: &quot;text-gray-300 hover:bg-gray-700 hover:text-white&quot;">Settings</a>

      <a href="/webtools?externalLoginKey=${externalLoginKey}"
         class="text-gray-300 hover:bg-gray-700 hover:text-white  px-3 py-2 rounded-md text-sm font-medium"
         x-state-description="undefined: &quot;bg-gray-900 text-white&quot;, undefined: &quot;text-gray-300 hover:bg-gray-700 hover:text-white&quot;">Webtools</a>

    </div>
    <div class="pt-4 pb-3 border-t border-gray-700">
      <div class="flex items-center px-5">
        <div class="flex-shrink-0">
          <img class="h-10 w-10 rounded-full"
               src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&amp;ixid=eyJhcHBfaWQiOjEyMDd9&amp;auto=format&amp;fit=facearea&amp;facepad=2&amp;w=256&amp;h=256&amp;q=80"
               alt="">
        </div>
        <div class="ml-3">
          <div class="text-base font-medium leading-none text-white">Tom Cook</div>
          <div class="text-sm font-medium leading-none text-gray-400">tom@example.com</div>
        </div>
      </div>
      <div class="mt-3 px-2 space-y-1">

<#--
        <a href="#"
           class="block px-3 py-2 rounded-md text-base font-medium text-gray-400 hover:text-white hover:bg-gray-700">Your
          Profile</a>

        <a href="#"
           class="block px-3 py-2 rounded-md text-base font-medium text-gray-400 hover:text-white hover:bg-gray-700">Settings</a>
-->

        <a href="<@ofbizUrl>logout</@ofbizUrl>"
           class="block px-3 py-2 rounded-md text-base font-medium text-gray-400 hover:text-white hover:bg-gray-700">Sign
          out</a>

      </div>
    </div>
  </div>
</nav>
