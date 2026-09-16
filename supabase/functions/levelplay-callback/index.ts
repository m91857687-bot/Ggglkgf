import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

// IronSource Server-to-Server Callback Edge Function
// Requires SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY to be set in Edge Function secrets.
// Also requires IRONSOURCE_SECRET_KEY for signature validation (optional but highly recommended).

serve(async (req) => {
  const url = new URL(req.url);
  const userId = url.searchParams.get("appUserId");
  const eventId = url.searchParams.get("eventId");
  const rewardsAmount = parseInt(url.searchParams.get("rewardsAmount") || "0");
  const signature = url.searchParams.get("sign");

  // Basic validation
  if (!userId || !eventId || rewardsAmount <= 0) {
    return new Response("Invalid parameters", { status: 400 });
  }

  // NOTE: In a production environment, calculate MD5(timestamp+eventId+userId+rewardsAmount+rewardsName+secretKey)
  // and compare it with the 'signature' parameter to prevent fake requests.

  try {
    // Create Supabase client with SERVICE_ROLE key to bypass RLS and insert securely.
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    // Insert transaction. The DB trigger will automatically update the coin_balance.
    // The unique constraint on event_id will prevent double crediting.
    const { error } = await supabaseClient
      .from("coin_transactions")
      .insert({
        user_id: userId,
        amount: rewardsAmount,
        type: "reward",
        event_id: eventId
      });

    if (error) {
      if (error.code === "23505") { // Unique violation (event_id already exists)
        return new Response("Event already processed: OK", { status: 200 });
      }
      throw error;
    }

    return new Response(`Successfully added ${rewardsAmount} coins to user ${userId}: OK`, { status: 200 });
  } catch (err) {
    console.error("Error processing callback:", err);
    return new Response("Internal Server Error", { status: 500 });
  }
})
